/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.server;

import java.net.Socket;
import org.apache.log4j.Logger;
import org.jppf.JPPFException;
import org.jppf.classloader.ResourceProvider;
import org.jppf.comm.socket.*;

/**
 * Wrapper around an incoming socket connection, whose role is to receive the names of classes
 * to load from the classpath, then send the class files' contents to the remote client.
 * <p>Instances of this class are part of the JPPF dynamic class loading mechanism. The enable remote nodes
 * to dynamically load classes from the JVM that run's the class server.
 * @author Laurent Cohen
 */
public abstract class JPPFConnection extends Thread
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(JPPFConnection.class);
	/**
	 * The socket client used to communicate over a socket connection.
	 */
	protected SocketWrapper socketClient = null;
	/**
	 * Indicates whether this socket handler should be terminated and stop processing.
	 */
	protected boolean stop = false;
	/**
	 * Indicates whether this socket handler is closed, which means it can't handle requests anymore.
	 */
	protected boolean closed = false;
	/**
	 * Reads resource files from the classpath.
	 */
	protected ResourceProvider resourceProvider = new ResourceProvider();
	/**
	 * The server that created this connection.
	 */
	protected JPPFServer server = null;

	/**
	 * Initialize this connection with an open socket connection to a remote client.
	 * @param socket the socket connection from which requests are received and to which responses are sent.
	 * @param server the class server that created this connection.
	 * @throws JPPFException if this socket handler can't be initialized.
	 */
	public JPPFConnection(JPPFServer server, Socket socket) throws JPPFException
	{
		this.server = server;
		socketClient = new SocketClient(socket);
	}
	
	/**
	 * Main processing loop for this socket handler. During each loop iteration,
	 * the following operations are performed:
	 * <ol>
	 * <li>if the stop flag is set to true, exit the loop</li>
	 * <li>block until an execution request is received</li>
	 * <li>when a request is received, dispatch it to the execution queue</li>
	 * <li>wait until the execution is complete</li>
	 * <li>send the execution result back to the client application</li>
	 * </ol>
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			while (!stop)
			{
				perform();
			}
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
			setClosed();
			server.removeConnection(this);
		}
	}

	/**
	 * Execute this thread's main action.
	 * @throws Exception if the execution failed.
	 */
	public abstract void perform() throws Exception;

	/**
	 * Set the stop flag to true, indicating that this socket handler should be closed as
	 * soon as possible.
	 */
	public synchronized void setStopped()
	{
		stop = true;
	}

	/**
	 * Determine whether the socket connection is closed
	 * @return true if the socket connection is closed, false otherwise
	 */
	public boolean isClosed()
	{
		return closed;
	}

	/**
	 * Set the closed state of the socket connection to true. This will cause this socket handler
	 * to terminate as soon as the current request execution is complete.
	 */
	public void setClosed()
	{
		setStopped();
		close();
	}

	/**
	 * Close the socket connection.
	 */
	public void close()
	{
		try
		{
			socketClient.close();
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
		closed = true;
	}

	/**
	 * Get a string representation of this connection.
	 * @return a string representation of this connection.
	 * @see java.lang.Thread#toString()
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (socketClient != null) sb.append(socketClient.getHost()).append(":").append(socketClient.getPort());
		else sb.append("socket is null");
		return sb.toString();
	}
}
