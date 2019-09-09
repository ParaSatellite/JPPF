/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.server.node;

import java.io.IOException;
import java.util.*;

import org.jppf.*;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.execute.async.ExecutionManagerListener;
import org.jppf.management.*;
import org.jppf.management.spi.*;
import org.jppf.node.connection.ConnectionReason;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.node.protocol.*;
import org.jppf.node.provisioning.SlaveNodeManager;
import org.jppf.node.throttling.NodeThrottlingHandler;
import org.jppf.persistence.JPPFDatasourceFactory;
import org.jppf.serialization.*;
import org.jppf.ssl.SSLConfigurationException;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.slf4j.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public abstract class JPPFNode extends AbstractCommonNode implements ClassLoaderProvider, ExecutionManagerListener {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The bundle currently processed in offline mode.
   */
  private Pair<TaskBundle, List<Task<?>>> currentBundle;
  /**
   * The slave node manager.
   */
  private final SlaveNodeManager slaveManager;
  /**
   * The mbean which sends notifications of configuration changes.
   */
  private final NodeConfigNotifier configNotifier = new NodeConfigNotifier();
  /**
   * Asynchronously receives new jobs.
   */
  private JobReader jobReader;
  /**
   * Asynchronously sends job results and notification bundles.
   */
  private JobWriter jobWriter;
  /**
   * Whether the execution of the current is complete (offline node only).
   */
  private boolean executionComplete;
  /**
   * Synchronizes read and write of the current job and its results.
   */
  private final ThreadSynchronization offlineLock = new ThreadSynchronization();
  /**
   * 
   */
  NodeThrottlingHandler throttlingHandler;
  /**
   * The uuid path of the handshake bundle.
   */
  private List<String> handshakeUuidPath;

  /**
   * Default constructor.
   * @param uuid this node's uuid.
   * @param configuration the configuration of this node.
   */
  public JPPFNode(final String uuid, final TypedProperties configuration) {
    super(uuid, configuration);
    if (debugEnabled) log.debug("creating execution manager");
    executionManager = new AsyncNodeExecutionManager(this);
    executionManager.addExecutionManagerListener(this);
    if (debugEnabled) log.debug("updating system information");
    updateSystemInformation();
    if (debugEnabled) log.debug("creating slave nodes manager");
    slaveManager = new SlaveNodeManager(this);
  }

  /**
   * Main processing loop of this node.
   * @exclude
   */
  @Override
  public void run() {
    setStopped(false);
    boolean initialized = false;
    if (debugEnabled) log.debug("start of node main loop, nodeUuid=" + uuid);
    while (!isStopped()) {
      try {
        if (!isLocal() && getShuttingDown().get()) break;
        init();
        if (!initialized) {
          System.out.println("node successfully initialized");
          initialized = true;
        }
        perform();
      } catch(final SecurityException|SSLConfigurationException e) {
        if (!isStopped()) reset(true);
        throw new JPPFError(e);
      } catch(final IOException e) {
        if (!getShuttingDown().get() && !isStopped()) log.error(e.getMessage(), e);
        if (!isStopped()) {
          reset(true);
          if (reconnectionNotification != null) {
            final JPPFReconnectionNotification tmp = reconnectionNotification;
            reconnectionNotification = null;
            throw tmp;
          }
          else throw new JPPFNodeReconnectionNotification("I/O exception occurred during node processing", e, ConnectionReason.JOB_CHANNEL_PROCESSING_ERROR);
        }
      } catch(final Exception e) {
        log.error(e.getMessage(), e);
        if (!isStopped()) reset(true);
      }
    }
    if (debugEnabled) log.debug("end of node main loop");
  }

  /**
   * Perform the main execution loop for this node. At each iteration, this method listens for a task to execute,
   * receives it, executes it and sends the results back.
   * @throws Exception if an error was raised from the underlying socket connection or the class loader.
   */
  private void perform() throws Exception {
    if (debugEnabled) log.debug("Start of node secondary loop");
    boolean shouldInitDataChannel = false;
    while (!checkStopped()) {
      clearResourceCachesIfRequested();
      if (isShutdownRequested()) shutdown(isRestart());
      else {
        try {
          while (isSuspended()) suspendedLock.goToSleep(1000L);
          if (shouldInitDataChannel) {
            shouldInitDataChannel = false;
            initDataChannel();
          }
          if (isOffline()) processNextJob();
          else processNextJobAsync();
        } catch (final IOException|JPPFSuspendedNodeException e) {
          if (!isSuspended()) throw e;
          shouldInitDataChannel = true;
        } finally {
          setExecuting(false);
        }
      }
    }
    if (debugEnabled) log.debug("End of node secondary loop");
  }

  /**
   * Read a job to execute or a handshake job.
   * @throws Exception if any error occurs.
   */
  private void processNextJob() throws Exception {
    final Pair<TaskBundle, List<Task<?>>> pair = nodeIO.readJob();
    if (debugEnabled) log.debug("received bundle");
    TaskBundle bundle = pair.first();
    List<Task<?>> taskList = pair.second();
    if (debugEnabled) log.debug(!bundle.isHandshake() ? "received a bundle with " + taskList.size()  + " tasks" : "received a handshake bundle");
    if (!bundle.isHandshake()) {
      currentBundle = pair;
      executionComplete = false;
      executionManager.execute(bundle, taskList);
      while (!isStopped() && !executionComplete) offlineLock.goToSleep();
      initDataChannel();
      processNextJob(); // new handshake
    } else {
      if (currentBundle != null) {
        bundle = currentBundle.first();
        taskList = currentBundle.second();
      }
      checkInitialBundle(bundle);
      currentBundle = null;
      processResults(bundle, taskList);
    }
  }

  /**
   * Read a job to execute or a handshake job.
   * @throws Exception if any error occurs.
   */
  private void processNextJobAsync() throws Exception {
    final Pair<TaskBundle, List<Task<?>>> pair = jobReader.nextJob();
    if (debugEnabled) log.debug("received bundle");
    final TaskBundle bundle = pair.first();
    final List<Task<?>> taskList = pair.second();
    if (debugEnabled) log.debug(!bundle.isHandshake() ? "received a bundle with " + taskList.size()  + " tasks" : "received a handshake bundle");
    if (!bundle.isHandshake()) {
      executionManager.execute(bundle, taskList);
    } else {
      checkInitialBundle(bundle);
      getJobWriter().putJob(bundle, taskList);
      if (isMasterNode()) slaveManager.handleStartup();
    }
  }

  /**
   * Checks whether the received bundle is the initial one sent by the driver,
   * and prepare a specific response if it is.
   * @param bundle the bundle to check.
   * @throws Exception if any error occurs.
   */
  private void checkInitialBundle(final TaskBundle bundle) throws Exception {
    checkStopped();
    if (debugEnabled) log.debug("setting initial bundle, offline=" + isOffline() + (currentBundle == null ? ", bundle=" + bundle : ", currentBundle=" + currentBundle.first()));
    bundle.setParameter(BundleParameter.NODE_UUID_PARAM, uuid);
    handshakeUuidPath = bundle.getUuidPath().getList();
    if (isOffline()) {
      bundle.setParameter(BundleParameter.NODE_OFFLINE, true);
      if (isSuspended()) bundle.setParameter(BundleParameter.CLOSE_COMMAND, true);
      if (currentBundle != null) {
        bundle.setParameter(BundleParameter.NODE_OFFLINE_OPEN_REQUEST, true);
        bundle.setBundleId(currentBundle.first().getBundleId());
        bundle.setParameter(BundleParameter.JOB_UUID, currentBundle.first().getUuid());
      }
    } else {
      if (!throttlingHandler.check()) bundle.setParameter(BundleParameter.NODE_ACCEPTS_NEW_JOBS, false);
      throttlingHandler.start();
    }
    if (isJmxEnabled()) setupBundleParameters(bundle);
    final Map<String, TypedProperties> defMap = bundle.getParameter(BundleParameter.DATASOURCE_DEFINITIONS, null);
    if (defMap != null) {
      if (debugEnabled) log.debug("got datasource definitions from server: {}", defMap.keySet());
      JPPFDatasourceFactory.getInstance().configure(defMap, this.getSystemInformation());
      bundle.removeParameter(BundleParameter.DATASOURCE_DEFINITIONS);
    }
  }

  /**
   * Send the results back to the server and perform final checks for the current execution.
   * @param bundle the bundle that contains the tasks and header information.
   * @param taskList the tasks results.
   * @throws Exception if any error occurs.
   */
  void processResults(final TaskBundle bundle, final List<Task<?>> taskList) throws Exception {
    checkStopped();
    currentBundle = null;
    if (debugEnabled) log.debug("processing " + (taskList == null ? 0 : taskList.size()) + " task results for job '" + bundle.getName() + '\'');
    if (executionManager.checkConfigChanged() || bundle.isHandshake() || isOffline()) {
      if (debugEnabled) log.debug("detected configuration change or initial bundle request, sending new system information to the server, config=\n{}", configuration);
      final TypedProperties jppf = systemInformation.getJppf();
      jppf.clear();
      jppf.putAll(configuration);
      bundle.setParameter(BundleParameter.SYSTEM_INFO_PARAM, systemInformation);
    }
    nodeIO.writeResults(bundle, taskList);
    if ((taskList != null) && (!taskList.isEmpty())) {
      if (!isJmxEnabled()) setExecutedTaskCount(getExecutedTaskCount() + taskList.size());
    }
    if (!bundle.isHandshake() && !bundle.isNotification()) lifeCycleEventHandler.fireBeforeNextJob();
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   * @exclude
   */
  protected synchronized void init() throws Exception {
    checkStopped();
    if (debugEnabled) log.debug("start node initialization");
    initHelper();
    if (debugEnabled) log.debug("creating LifeCycleEventHandler");
    try {
      lifeCycleEventHandler = new LifeCycleEventHandler(this);
    } catch (final Throwable e) {
      log.error(e.getMessage(), e);
    }
    try {
      if (ManagementUtils.isManagementAvailable() && !ManagementUtils.isMBeanRegistered(JPPFNodeAdminMBean.MBEAN_NAME)) {
        final ClassLoader cl = getClass().getClassLoader();
        if (providerManager == null) providerManager = new JPPFMBeanProviderManager<>(JPPFNodeMBeanProvider.class, cl, ManagementUtils.getPlatformServer(), this);
      }
    } catch (final Exception e) {
      log.error("Error registering the MBeans", e);
    }
    if (isJmxEnabled()) {
      try {
        getJmxServer();
      } catch(final Exception e) {
        jmxEnabled = false;
        System.out.println("JMX initialization failure - management is disabled for this node\nsee the log file for details");
        log.error("Error creating the JMX server", e);
        try {
          if (jmxServer != null) jmxServer.stop();
        } catch(final Exception e2) {
          log.error("Error stopping the JMX server", e2);
        }
      }
    }
    initStartups();
    initDataChannel();
    lifeCycleEventHandler.loadProviders();
    lifeCycleEventHandler.fireNodeStarting();
    if (!isOffline()) {
      ThreadUtils.startDaemonThread(jobReader = new JobReader(this), "JobReader");
      ThreadUtils.startDaemonThread(jobWriter = new JobWriter(this), "JobWriter");
    }
    throttlingHandler = new NodeThrottlingHandler(this);
    if (debugEnabled) log.debug("end node initialization");
  }

  /**
   * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
   * @throws Exception if an error occurs while instantiating the class loader.
   * @exclude
   */
  public void initHelper() throws Exception {
    final AbstractJPPFClassLoader cl = getClassLoader();
    if (debugEnabled) log.debug("Initializing serializer using {}", cl);
    Class<?> c = cl.loadJPPFClass("org.jppf.utils.ObjectSerializerImpl");
    if (debugEnabled) log.debug("Loaded serializer class {}", c);
    serializer = (ObjectSerializer) c.newInstance();
    c = cl.loadJPPFClass("org.jppf.utils.SerializationHelperImpl");
    if (debugEnabled) log.debug("Loaded helper class {}", c);
    helper = (SerializationHelper) c.newInstance();
    if (debugEnabled) log.debug("Serializer initialized");
  }

  /**
   * Trigger the configuration changed flag.
   * @exclude
   */
  public void triggerConfigChanged() {
    updateSystemInformation();
    executionManager.triggerConfigChanged();
  }

  @Override
  public ClassLoader getClassLoader(final List<String> uuidPath) throws Exception {
    return getContainer(uuidPath).getClassLoader();
  }

  /**
   * Check whether this node is stopped or shutting down. If not, an unchecked {@code IllegalStateException} is thrown.
   * @return {@code true} if the node is stopped or shutting down.
   */
  private boolean checkStopped() {
    if (isStopped()) throw new IllegalStateException("this node is shutting down");
    return false;
  }

  /**
   * @return the slave node manager.
   * @exclude
   */
  public SlaveNodeManager getSlaveManager() {
    return slaveManager;
  }

  @Override
  public NodeConfigNotifier getNodeConfigNotifier() {
    return configNotifier;
  }

  @Override
  public void bundleExecuted(final TaskBundle bundle, final List<Task<?>> tasks, final Throwable t) {
    try {
      if (debugEnabled) log.debug("executed {} tasks of job {}", tasks.size(), bundle);
      if (isOffline()) {
        executionComplete = true;
        offlineLock.wakeUp();
      } else {
        getJobWriter().putJob(bundle, tasks);
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * @exclude
   */
  @Override
  public synchronized void stopNode() {
    if (debugEnabled) log.debug("stopping node");
    if (jobReader != null) jobReader.close();
    if (getJobWriter() != null) getJobWriter().close();
    throttlingHandler.stop();
    super.stopNode();
    slaveManager.stopAllSlaves();
  }

  /**
   * @return the object which asynchronously sends job results and notification bundles.
   * @exclude
   */
  public JobWriter getJobWriter() {
    return jobWriter;
  }

  /**
   * @return the uuid path of the handshake bundle.
   */
  public List<String> getHandshakeUuidPath() {
    return handshakeUuidPath;
  }
}
