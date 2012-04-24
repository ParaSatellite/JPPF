/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.util.List;

import org.jppf.*;
import org.jppf.node.*;
import org.jppf.node.protocol.Task;

/**
 * Wrapper around a JPPF task used to catch exceptions caused by the task execution.
 * @author Domingos Creado
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
class NodeTaskWrapper extends AbstractNodeTaskWrapper
{
  /**
   * The execution manager.
   */
  private final NodeExecutionManager executionManager;
  /**
   * The class loader instance.
   */
  private final ClassLoader classLoader;

  /**
   * Initialize this task wrapper with a specified JPPF task.
   * @param executionManager reference to the execution manager.
   * @param task the task to execute within a try/catch block.
   * @param uuidPath the key to the JPPFContainer for the task's class loader.
   * @param number the internal number identifying the task for the thread pool.
   * @param classLoader the class loader used as context class loader.
   */
  public NodeTaskWrapper(final NodeExecutionManager executionManager, final Task task, final List<String> uuidPath,
                         final long number, final ClassLoader classLoader)
  {
    super(task, uuidPath, number);
    this.executionManager = executionManager;
    this.classLoader = classLoader;
  }

  /**
   * Get the number identifying the task.
   * @return long value identifying the task.
   */
  public long getNumber()
  {
    return number;
  }

  /**
   * Execute the task within a try/catch block.
   * @see Runnable#run()
   */
  @Override
  public void run()
  {
    JPPFNodeReconnectionNotification reconnectionNotification = null;
    ThreadManager threadManager = executionManager.getThreadManager();
    NodeExecutionInfo info = null;
    NodeExecutionInfo info2 = null;
    long elapsedTime = 0L;
    try
    {
      if (classLoader != null) Thread.currentThread().setContextClassLoader(classLoader);
      long id = Thread.currentThread().getId();
      long startTime = System.nanoTime();
      info = threadManager.computeExecutionInfo(id);
      synchronized(task)
      {
        task.run();
      }
      try
      {
        // convert cpu time from nanoseconds to milliseconds
        if (info != null) info2 = threadManager.computeExecutionInfo(id).subtract(info);
        elapsedTime = (System.nanoTime() - startTime) / 1000000L;
      }
      catch(Throwable ignore)
      {
      }
    }
    catch(JPPFNodeReconnectionNotification t)
    {
      reconnectionNotification = t;
    }
    catch(Throwable t)
    {
      if (t instanceof Exception) task.setException((Exception) t);
      else task.setException(new JPPFException(t));
    }
    finally
    {
      if (reconnectionNotification == null)
      {
        try
        {
          executionManager.taskEnded(task, number, info2, elapsedTime);
        }
        catch(JPPFNodeReconnectionNotification t)
        {
          reconnectionNotification = t;
        }
      }
      if (reconnectionNotification != null)
      {
        executionManager.setReconnectionNotification(reconnectionNotification);
      }
    }
  }
}
