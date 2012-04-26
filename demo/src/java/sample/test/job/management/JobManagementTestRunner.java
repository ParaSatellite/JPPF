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

package sample.test.job.management;

import java.util.List;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

import sample.dist.tasklength.LongTask;

/**
 * 
 * @author Laurent Cohen
 */
public class JobManagementTestRunner
{
  /**
   * The JPPF client.
   */
  private static JPPFClient client = null;
  /**
   * 
   */
  private static JMXDriverConnectionWrapper driver = null;

  /**
   * Run the first test.
   * @param jobName name given to the job.
   * @throws Exception if any error occurs.
   */
  public void runTest1(final String jobName) throws Exception
  {
    TypedProperties props = JPPFConfiguration.getProperties();
    int nbTasks = props.getInt("job.management.nbTasks", 2);
    long duration = props.getLong("job.management.duration", 1000L);
    JPPFJob job = new JPPFJob(jobName);
    job.setName(jobName);
    job.setBlocking(false);
    for (int i=0; i<nbTasks; i++)
    {
      JPPFTask task = new LongTask(duration);
      task.setId(jobName + " - task " + i);
      job.addTask(task);
    }
    JPPFResultCollector collector = new JPPFResultCollector(job);
    job.setResultListener(collector);
    client.submit(job);
    DriverJobManagementMBean proxy = driver.getProxy(DriverJobManagementMBean.MBEAN_NAME, DriverJobManagementMBean.class);
    // wait to ensure the job has been dispatched to the nodes
    Thread.sleep(1000);
    driver.cancelJob(job.getUuid());
    List<JPPFTask> results = collector.waitForResults();
    for (JPPFTask task: results)
    {
      Exception e = task.getException();
      if (e != null) System.out.println("" + task.getId() + " has an exception: " + ExceptionUtils.getStackTrace(e));
      else System.out.println("Result for " + task.getId() + ": " + task.getResult());
    }
    System.out.println("Test ended");
  }

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      System.out.println("Initializing client ...");
      client = new JPPFClient("client");
      System.out.println("Awaiting server connection ...");
      while (!client.hasAvailableConnection()) Thread.sleep(100L);
      System.out.println("Awaiting JMX connection ...");
      driver = ((JPPFClientConnectionImpl) client.getClientConnection()).getJmxConnection();
      while (!driver.isConnected()) driver.connectAndWait(100L);
      JobManagementTestRunner runner = new JobManagementTestRunner();
      System.out.println("Running test 1 ...");
      runner.runTest1("job1");
      System.out.println("Running test 2 ...");
      runner.runTest1("job2");
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (driver != null)
      {
        try
        {
          driver.close();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
      if (client != null) client.close();
    }
  }
}
