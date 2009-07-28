/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.node;

import java.lang.management.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;

/**
 * Instances of this class manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 */
public class NodeExecutionManager extends ThreadSynchronization
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeExecutionManager.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The Thread Pool that really process the tasks
	 */
	private ThreadPoolExecutor threadPool = null;
	/**
	 * The factory used to create thread in the pool.
	 */
	private JPPFThreadFactory threadFactory = null;
	/**
	 * The node that uses this excecution manager.
	 */
	private JPPFNode node = null;
	/**
	 * Timer managing the tasks timeout.
	 */
	private Timer timeoutTimer = null;
	/**
	 * Map of futures to corresponding timeout timer tasks.
	 */
	private Map<Future<?>, TimerTask> timerTaskMap = new Hashtable<Future<?>, TimerTask>();
	/**
	 * Mapping of tasks numbers to their id.
	 */
	//private Map<String, List<Pair<Long, JPPFTask>>> idMap = new Hashtable<String, List<Pair<Long, JPPFTask>>>();
	private Map<String, List<Long>> idMap = new Hashtable<String, List<Long>>();
	/**
	 * Mapping of internal number to the coresponding tasks.
	 */
	private Map<Long, JPPFTask> taskMap = new Hashtable<Long, JPPFTask>();
	/**
	 * The bundle whose tasks are currently being executed.
	 */
	private JPPFTaskBundle bundle = null;
	/**
	 * The uuid path of the current bundle.
	 */
	private List<String> uuidList = null;
	/**
	 * Holds a the set of futures generated by submitting each task. 
	 */
	private Map<Long, Future<?>> futureMap = new Hashtable<Long, Future<?>>();
	/**
	 * Counter for the number of tasks whose execution is over,
	 * including tasks that were completed normally, were cancelled or timed out. 
	 */
	private AtomicLong taskCount = new AtomicLong(0L);
	/**
	 * The platform MBean used to gather statistics about the JVM threads.
	 */
	private ThreadMXBean threadMXBean = null;
	/**
	 * Determines wheather the thread cpu time measurement is supported and enabled.
	 */
	private boolean cpuTimeEnabled = false;

	/**
	 * Initialize this execution manager with the specified node.
	 * @param node the node that uses this excecution manager.
	 */
	public NodeExecutionManager(JPPFNode node)
	{
		this.node = node;
		TypedProperties props = JPPFConfiguration.getProperties();
		int poolSize = props.getInt("processing.threads", Runtime.getRuntime().availableProcessors());
		int priority = props.getInt("processing.threads.priority", Thread.NORM_PRIORITY);
		log.info("Node running " + poolSize + " processing thread" + (poolSize > 1 ? "s" : ""));
		threadMXBean = ManagementFactory.getThreadMXBean();
		cpuTimeEnabled = threadMXBean.isThreadCpuTimeSupported();
		threadFactory = new JPPFThreadFactory("node processing thread", cpuTimeEnabled);
		LinkedBlockingQueue queue = new LinkedBlockingQueue();
		threadPool = new ThreadPoolExecutor(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.MICROSECONDS, queue, threadFactory);
		timeoutTimer = new Timer("Node Task Timeout Timer");
		if (debugEnabled) log.debug("thread cpu time supported = " + cpuTimeEnabled);
		if (cpuTimeEnabled) threadMXBean.setThreadCpuTimeEnabled(true);
	}

	/**
	 * Execute the specified tasks of the specified tasks bundle.
	 * @param bundle the bundle to which the tasks are associated.
	 * @param taskList the list of tasks to execute.
	 * @throws Exception if the execution failed.
	 */
	public void execute(JPPFTaskBundle bundle, List<JPPFTask> taskList) throws Exception
	{
		if (debugEnabled) log.debug("executing " + taskList.size() + " tasks");
		NodeExecutionInfo info = null;
		if (cpuTimeEnabled) info = computeExecutionInfo();
		setup(bundle);
		for (JPPFTask task : taskList) performTask(task);
		waitForResults();
		cleanup();
		if (cpuTimeEnabled)
		{
			NodeExecutionInfo info2 = computeExecutionInfo();
			info2.cpuTime -= info.cpuTime;
			info2.userTime -= info.userTime;
			if (debugEnabled) log.debug("total cpu time used: " + info2.cpuTime + " ms, user time: " + info2.userTime);
		}
	}

	/**
	 * Execute a single task.
	 * @param task the task to execute.
	 * @return a number identifying the task that was submitted.
	 * @throws Exception if the execution failed.
	 */
	public synchronized long performTask(JPPFTask task) throws Exception
	{
		String id = task.getId();
		long number = incTaskCount();
		taskMap.put(number, task);
		//if (debugEnabled) log.debug("submitting task with number " + number);
		Future<?> f = threadPool.submit(new NodeTaskWrapper(node, task, uuidList, number));
		if (!f.isDone()) futureMap.put(number, f);
		if (id != null)
		{
			List<Long> pairList = idMap.get(id);
			if (pairList == null)
			{
				pairList = new ArrayList<Long>();
				idMap.put(id, pairList);
			}
			pairList.add(number);
		}
		if ((task.getTimeout() > 0L) || (task.getTimeoutDate() != null))
		{
			processTaskTimeout(task, number);
		}
		return number;
	}

	/**
	 * Cancel all executing or pending tasks.
	 * @param callOnCancel - determines whether the onCancel() callback method of each task should be invoked.
	 */
	public synchronized void cancelAllTasks(boolean callOnCancel)
	{
		List<Long> list = new ArrayList<Long>(futureMap.keySet());
		for (Long n: list) cancelTask(n, callOnCancel);
	}

	/**
	 * Cancel the execution of the tasks with the specified id.
	 * @param id the id of the tasks to cancel.
	 */
	public synchronized void cancelTask(String id)
	{
		if (debugEnabled) log.debug("cancelling tasks with id = " + id);
		List<Long> numberList = idMap.remove(id);
		if (numberList == null) return;
		if (debugEnabled) log.debug("number of tasks to cancel: " + numberList.size());
		for (Long number: numberList) cancelTask(number, true);
	}

	/**
	 * Cancel the execution of the tasks with the specified id.
	 * @param number - the index of the task to cancel.
	 * @param callOnCancel - determines whether the onCancel() callback method of each task should be invoked.
	 */
	private synchronized void cancelTask(Long number, boolean callOnCancel)
	{
		if (debugEnabled) log.debug("cancelling task number = " + number);
		Future<?> future = futureMap.get(number);
		if (!future.isDone())
		{
			future.cancel(true);
			JPPFTask task = taskMap.remove(number);
			if (task != null) task.onCancel();
			removeFuture(number);
		}
	}

	/**
	 * Restart the execution of the tasks with the specified id.<br>
	 * The task(s) will be restarted even if their execution has already completed.
	 * @param id the id of the task or tasks to restart.
	 */
	public synchronized void restartTask(String id)
	{
		if (debugEnabled) log.debug("restarting tasks with id = " + id);
		List<Long> numberList = idMap.remove(id);
		if (numberList == null) return;
		if (debugEnabled) log.debug("number of tasks to restart: " + numberList.size());
		for (Long number: numberList)
		{
			Future<?> future = futureMap.get(number);
			if (!future.isDone())
			{
				future.cancel(true);
				JPPFTask task = taskMap.remove(number);
				removeFuture(number);
				if (task != null)
				{
					task.onRestart();
					try
					{
						performTask(task);
					}
					catch(Exception e)
					{
						log.error(e.getMessage(), e);
					}
				}
			}
		}
	}

	/**
	 * Notify the timer that a task must be aborted if its timeout period expired.
	 * @param task the JPPF task for which to set the timeout.
	 * @param number a number identifying the task submitted to the thread pool.
	 */
	private void processTaskTimeout(JPPFTask task, long number)
	{
		long time = 0L;
		if (task.getTimeout() > 0) time = task.getTimeout();
		else
		{
			String date = task.getTimeoutDate();
			SimpleDateFormat sdf = task.getTimeoutDateFormat();
			try
			{
				time = sdf.parse(date).getTime() - System.currentTimeMillis();
			}
			catch(ParseException e)
			{
				log.error("Unparseable timeout date: " + date + ", format = " + sdf.toPattern(), e);
			}
		}
		if (time > 0L)
		{
			TimerTask tt = new TimeoutTimerTask(this, number, task);
			timerTaskMap.put(getFutureFromNumber(number), tt);
			timeoutTimer.schedule(tt, time);
		}
	}

	/**
	 * Shutdown this execution manager.
	 */
	public void shutdown()
	{
		threadPool.shutdownNow();
		if (timeoutTimer != null) timeoutTimer.cancel();
		timerTaskMap.clear();
	}

	/**
	 * Set the size of the node's thread pool.
	 * @param size the size as an int.
	 */
	public void setThreadPoolSize(int size)
	{
		if (size <= 0)
		{
			log.warn("ignored attempt to set the thread pool size to 0 or less: " + size);
			return;
		}
		int n = getThreadPoolSize();
		if (n == size) return;
		ThreadPoolExecutor tpe = (ThreadPoolExecutor) threadPool;
		if (size > tpe.getCorePoolSize())
		{
			tpe.setMaximumPoolSize(size);
			tpe.setCorePoolSize(size);
		}
		else if (size < tpe.getCorePoolSize())
		{
			tpe.setCorePoolSize(size);
			tpe.setMaximumPoolSize(size);
		}
		log.info("Node thread pool size changed from " + n + " to " + size);
		JPPFConfiguration.getProperties().setProperty("processing.threads", "" + size);
	}

	/**
	 * Get the size of the node's thread pool.
	 * @return the size as an int.
	 */
	public int getThreadPoolSize()
	{
		if (threadPool == null) return 0;
		return ((ThreadPoolExecutor) threadPool).getCorePoolSize();
	}

	/**
	 * Get the total cpu time used by the task processing threads.
	 * @return the cpu time on milliseconds.
	 */
	public long getCpuTime()
	{
		if (!cpuTimeEnabled) return 0L;
		return computeExecutionInfo().cpuTime;
	}

	/**
	 * Prepare this execution manager for executing the tasks of a bundle.
	 * @param bundle the bundle whose tasks are to be executed.
	 */
	public void setup(JPPFTaskBundle bundle)
	{
		this.bundle = bundle;
		this.uuidList = bundle.getUuidPath().getList();
		taskCount = new AtomicLong(0L);
	}

	/**
	 * Cleanup method invoked when all tasks for the current bundle have completed.
	 */
	public void cleanup()
	{
		this.bundle = null;
		this.uuidList = null;
		futureMap.clear();
		taskMap.clear();
		timerTaskMap.clear();
		idMap.clear();
		timeoutTimer.purge();
	}

	/**
	 * Wait until all tasks are complete.
	 * @throws Exception if the execution failed.
	 */
	public synchronized void waitForResults() throws Exception
	{
		while (!futureMap.isEmpty()) goToSleep();
	}

	/**
	 * Remove the specified future from the pending set and notify
	 * all threads waiting for the end of the execution.
	 * @param number task identifier for the future of the task to remove.
	 */
	public synchronized void removeFuture(long number)
	{
		Future<?> future = futureMap.remove(number);
		//if (debugEnabled) log.debug("removing task with number " + number + ", future = " + future);
		TimerTask tt = future == null ? null : timerTaskMap.remove(future);
		if (tt != null) tt.cancel();
		wakeUp();
	}

	/**
	 * Increment the current task count and return the new value.
	 * @return the new valueas a long.
	 */
	private long incTaskCount()
	{
		return taskCount.incrementAndGet();
	}

	/**
	 * Notifiaction sent by a node task wrapper when a task is complete.
	 * @param taskNumber the number identifying the task.
	 */
	public void taskEnded(long taskNumber)
	{
		removeFuture(taskNumber);
	}

	/**
	 * Get the future corresponding to the specified task number.
	 * @param number the number identifying the task.
	 * @return a <code>Future</code> instance.
	 */
	public synchronized Future<?> getFutureFromNumber(long number)
	{
		return futureMap.get(number);
	}

	/**
	 * Computes the total CPU time used by the execution threads.
	 * @return a <code>NodeExecutionInfo</code> instance.
	 */
	private NodeExecutionInfo computeExecutionInfo()
	{
		if (!cpuTimeEnabled) return null;
		NodeExecutionInfo info = new NodeExecutionInfo();
		List<Long> ids = threadFactory.getThreadIDs();
		for (Long id: ids)
		{
			info.cpuTime += threadMXBean.getThreadCpuTime(id);
			info.userTime += threadMXBean.getThreadUserTime(id);
		}
		info.cpuTime /= 1e6;
		info.userTime /= 1e6;
		return info;
	}

	/**
	 * Get the priority assigned to the execution threads.
	 * @return the priority as an int value.
	 */
	public int getThreadsPriority()
	{
		return threadFactory.getPriority();
	}

	/**
	 * Update the priority of all execution threads.
	 * @param newPriority the new priority to set.
	 */
	public void updateThreadsPriority(int newPriority)
	{
		threadFactory.updatePriority(newPriority);
	}

	/**
	 * Get the id of the job currently being executed.
	 * @return the job id as a string, or null if no job is being executed.
	 */
	public String getCurrentJobId()
	{
		return (bundle != null) ? (String) bundle.getParameter(BundleParameter.JOB_ID) : null;
	}
}
