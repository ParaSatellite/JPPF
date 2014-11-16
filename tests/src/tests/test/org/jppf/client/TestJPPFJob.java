/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package test.org.jppf.client;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.*;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>JPPFJob</code>.
 * @author Laurent Cohen
 */
public class TestJPPFJob extends Setup1D1N {
  /**
   * Test that {@link Task#getTaskObject()} always returns the expected object.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testGetTaskObject() throws Exception {
    JPPFJob job = new JPPFJob();
    Task<?> task = job.add(new SimpleRunnable());
    assertNotNull(task);
    assertNotNull(task.getTaskObject());
    Task<?> task2 = job.add(new SimpleTask());
    assertNotNull(task2);
    assertNotNull(task2.getTaskObject());
  }

  /**
   * Test that the expected number of {@link org.jppf.client.event.JobListener} notifications are received in the expected order,
   * with local execution enbaled and remote execution disabled.
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testJobListenerLocalExecution() throws Exception {
    int nbTasks = 10;
    TypedProperties props = JPPFConfiguration.getProperties();
    props.setBoolean("jppf.remote.execution.enabled", false);
    props.setBoolean("jppf.local.execution.enabled", true);
    props.setInt("jppf.local.execution.threads", 4);
    props.setString("jppf.load.balancing.algorithm", "manual");
    props.setString("jppf.load.balancing.profile", "manual");
    props.setInt("jppf.load.balancing.profile.manual.size", 5);
    try (JPPFClient client = BaseSetup.createClient(null, false)) {
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, 50L);
      CountingJobListener listener = new CountingJobListener();
      job.addJobListener(listener);
      client.submitJob(job);
      //Thread.sleep(500L);
      assertEquals(1, listener.startedCount.get());
      assertEquals(1, listener.endedCount.get());
    }
  }

  /**
   * Test that a job can cancel itself.
   * @throws Exception if any error occurs
   */
  @SuppressWarnings("deprecation")
  @Test(timeout=10000)
  public void testCancel() throws Exception {
    try (JPPFClient client = BaseSetup.createClient(null, true)) {
      int nbTasks = 10;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, false, nbTasks, LifeCycleTask.class, 5000L);
      ExecutingJobStatusListener statusListener = new ExecutingJobStatusListener();
      ((JPPFResultCollector) job.getResultListener()).addSubmissionStatusListener(statusListener);
      client.submitJob(job);
      statusListener.await();
      boolean cancelled = job.cancel(true);
      assertTrue(cancelled);
      List<Task<?>> results = job.get();
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      int count = 0;
      for (Task<?> task: results) {
        if (task.getResult() == null) count++;
      }
      assertTrue(count > 0);
      assertTrue(job.isCancelled());
      assertTrue(job.isDone());
    }
  }

  /**
   * Test that a job isn't cancelled and {@code job.cancel(false)} is invoked while it is executing.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("deprecation")
  @Test(timeout=15000)
  public void testCancelWithInterruptFlagFalse() throws Exception {
    try (JPPFClient client = BaseSetup.createClient(null, true)) {
      int nbTasks = 1;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, false, nbTasks, LifeCycleTask.class, 3000L);
      ExecutingJobStatusListener statusListener = new ExecutingJobStatusListener();
      ((JPPFResultCollector) job.getResultListener()).addSubmissionStatusListener(statusListener);
      client.submitJob(job);
      statusListener.await();
      boolean cancelled = job.cancel(false);
      assertFalse(cancelled);
      List<Task<?>> results = job.get();
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      int count = 0;
      for (Task<?> task: results) {
        if (task.getResult() == null) count++;
      }
      assertEquals(0, count);
      assertFalse(job.isCancelled());
      assertTrue(job.isDone());
    }
  }

  /**
   * Test that when {@code JPPFJob.get(timeout)} expires, a {@link TimeoutException} is raised.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000, expected=TimeoutException.class)
  public void testGetWithTimeout() throws Exception {
    try (JPPFClient client = BaseSetup.createClient(null, true)) {
      int nbTasks = 1;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, false, nbTasks, LifeCycleTask.class, 2000L);
      client.submitJob(job);
      job.get(1000L, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Test that when a job expires to due its client side SLA's expiration schedule, {@code JPPFJob.isCancelled()} returns {@code true}.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testIsCancelledClientSideExpiration() throws Exception {
    try (JPPFClient client = BaseSetup.createClient(null, true)) {
      int nbTasks = 1;
      long duration = 3000L;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, LifeCycleTask.class, duration);
      job.getClientSLA().setJobExpirationSchedule(new JPPFSchedule(duration/2L));
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      int count = 0;
      for (Task<?> task: results) {
        if (task.getResult() == null) count++;
      }
      assertEquals(1, count);
      assertTrue(job.isCancelled());
      assertTrue(job.isDone());
    }
  }
}
