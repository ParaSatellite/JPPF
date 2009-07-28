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
package org.jppf.server.job.management;

import java.nio.channels.SelectableChannel;

import org.apache.commons.logging.*;
import org.jppf.management.*;
import org.jppf.server.JPPFDriver;

/**
 * Instances of this class are intented to perform job management functions for a specxific node. 
 * @author Laurent Cohen
 */
public class CancelJobTask implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(CancelJobTask.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The id of the job to manage.
	 */
	private String jobId = null;
	/**
	 * The node on which to perform this task.
	 */
	private SelectableChannel channel = null;

	/**
	 * Initialize this task.
	 * @param jobId - the id of the job to manage.
	 * @param channel - the node on which to perform this task.
	 */
	public CancelJobTask(String jobId, SelectableChannel channel)
	{
		this.jobId = jobId;
		this.channel = channel;
	}

	/**
	 * Execute this task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			if (debugEnabled) log.debug("Request to cancel jobId = '" + jobId + "' on node " + channel);
			NodeManagementInfo nodeInfo = JPPFDriver.getInstance().getNodeInformation(channel);
			if (nodeInfo == null) return;
			JMXNodeConnectionWrapper node = new JMXNodeConnectionWrapper(nodeInfo.getHost(), nodeInfo.getPort());
			node.connect();
			while (!node.isConnected()) Thread.sleep(10);
			node.invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "cancelJob", new Object[] { jobId }, new String[] { "java.lang.String" });
			node.close();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
