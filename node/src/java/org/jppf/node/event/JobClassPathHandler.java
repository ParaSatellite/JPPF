/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.node.event;

import java.io.File;
import java.net.URL;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.node.Node;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This NodeLifeCycleListener implementations parses the classpath associated with a job
 *  and add its elemenents to the task class laoder.
 * @author Laurent Cohen
 * @exclude
 */
public class JobClassPathHandler extends DefaultLifeCycleErrorHandler implements NodeLifeCycleListener
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JobClassPathHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event)
  {
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event)
  {
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event)
  {
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event)
  {
  }

  @Override
  public void jobHeaderLoaded(final NodeLifeCycleEvent event)
  {
    if (log.isTraceEnabled()) log.trace(StringUtils.printClassLoaderHierarchy(event.getTaskClassLoader()));
    ClassPath classpath = event.getJob().getSLA().getClassPath();
    Node node = event.getNode();
    if ((classpath != null) && !classpath.isEmpty() && node.isOffline())
    {
      AbstractJPPFClassLoader cl = node.resetTaskClassLoader();
      for (ClassPathElement elt: classpath)
      {
        boolean validated = false;
        try
        {
          validated = elt.validate();
        }
        catch (Throwable t)
        {
          String format = "exception occurred during validation of classpath element '{}' : {}";
          if (debugEnabled) log.debug(format, elt, ExceptionUtils.getStackTrace(t));
          else log.warn(format, elt, ExceptionUtils.getMessage(t));
        }
        if (!validated) continue;
        URL url = null;
        Location location = elt.getLocation();
        try
        {
          if (location instanceof MemoryLocation)
          {
            cl.getResourceCache().registerResource(elt.getName(), location);
            url = cl.getResourceCache().getResourceURL(elt.getName());
          }
          else if (location instanceof FileLocation)
          {
            File file = new File(((FileLocation) location).getPath());
            if (file.exists()) url = file.toURI().toURL();
          }
          else if (location instanceof URLLocation) url = ((URLLocation) location).getPath();
        }
        catch (Exception e)
        {
          String format = "exception occurred during processing of classpath element '{}' : {}";
          if (debugEnabled) log.debug(format, elt, ExceptionUtils.getStackTrace(e));
          else log.warn(format, elt, ExceptionUtils.getMessage(e));
        }
        if (url != null) cl.addURL(url);
      }
      classpath.clear();
    }
  }
}