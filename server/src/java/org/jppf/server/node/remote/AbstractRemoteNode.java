/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
package org.jppf.server.node.remote;

import org.jppf.comm.recovery.*;
import org.jppf.node.connection.DriverConnectionInfo;
import org.jppf.server.node.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @since 5.1
 * @author Laurent Cohen
 */
public abstract class AbstractRemoteNode extends JPPFNode implements ClientConnectionListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractRemoteNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Connection to the recovery server.
   */
  private ClientConnection recoveryConnection = null;
  /**
   * Server connection information.
   */
  private final DriverConnectionInfo connectionInfo;

  /**
   * Default constructor.
   * @param connectionInfo the server connection information.
   */
  public AbstractRemoteNode(final DriverConnectionInfo connectionInfo) {
    super();
    this.connectionInfo = connectionInfo;
    initClassLoaderManager();
  }

  /**
   * Create and initialize the {@link AbstractClassLoaderManager class loader manager}.
   */
  protected abstract void initClassLoaderManager();

  @Override
  public void initDataChannel() throws Exception {
    TypedProperties config = JPPFConfiguration.getProperties();
    (nodeConnection = new RemoteNodeConnection(connectionInfo, serializer)).init();
    if (nodeIO == null) nodeIO = new RemoteNodeIO(this);
    if (config.get(JPPFProperties.RECOVERY_ENABLED)) {
      if (recoveryConnection == null) {
        if (debugEnabled) log.debug("Initializing recovery");
        recoveryConnection = new ClientConnection(uuid, connectionInfo.getHost(), connectionInfo.getRecoveryPort());
        recoveryConnection.addClientConnectionListener(this);
        new Thread(recoveryConnection, "reaper client connection").start();
      }
    }
  }

  @Override
  public void closeDataChannel() throws Exception {
    if (debugEnabled) log.debug("closing data channel: nodeConnection=" + nodeConnection + ", recoveryConnection=" + recoveryConnection);
    if (nodeConnection != null) nodeConnection.close();
    if (recoveryConnection != null) {
      ClientConnection tmp = recoveryConnection;
      if (tmp != null) {
        recoveryConnection = null;
        tmp.close();
      }
    }
  }

  @Override
  public void clientConnectionFailed(final ClientConnectionEvent event) {
    try {
      if (debugEnabled) log.debug("recovery connection failed, attempting to reconnect this node");
      closeDataChannel();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  protected NodeConnectionChecker createConnectionChecker() {
    return new RemoteNodeConnectionChecker(this);
  }

  @Override
  public boolean isLocal() {
    return false;
  }
}