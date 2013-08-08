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

package org.jppf.jca.spi;

import java.io.*;
import java.net.URL;

import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * This class is used to parse the JPPF configuration from the properties
 * qspecified in the deployment descriptor of the resource adapter.
 * @author Laurent Cohen
 */
class JPPFConfigurationParser
{
  /**
   * The default configuration source.
   */
  private static final String DEFAULT_CONFIG_SOURCE = "classptah|jppf.properties";
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFConfigurationParser.class);
  /**
   * A string holding the client configuration, specified as a property in the ra.xml descriptor.
   */
  private final String clientConfiguration;
  /**
   * Defines how the configuration is to be located.<br>
   * This property is defined in the format "<i>type</i>|<i>path</i>", where <i>type</i> can be one of:<br>
   * <ul>
   * <li>"classpath": in this case <i>path</i> is a path to a properties file in one of the jars of the .rar file, for instance "resources/config/jppf.properties"</li>
   * <li>"url": <i>path</i> is a url that points to a properties file, for instance "file:///home/me/jppf/jppf.properties" (could be a http:// or ftp:// url as well)</li>
   * <li>"file": <i>path</i> is considered a path on the file system, for instance "/home/me/jppf/config/jppf.properties"</li>
   * </ul>
   */
  private final String configurationSource;

  /**
   * Initialize this ocnfiguration parser.
   * @param configurationSource a string holding the client configuration.
   * @param clientConfiguration defines how the configuration is to be located.
   */
  public JPPFConfigurationParser(final String configurationSource, final String clientConfiguration)
  {
    this.configurationSource = configurationSource;
    this.clientConfiguration = clientConfiguration;
  }

  /**
   * Parse the ocnfiguration.
   * @return the config as a {@link TypedProperties} object.
   */
  public TypedProperties parse()
  {
    String src = configurationSource;
    if (src != null) src = src.trim();
    if ((src == null) || src.isEmpty())
    {
      if ((clientConfiguration == null) || clientConfiguration.trim().isEmpty()) src = DEFAULT_CONFIG_SOURCE;
      else return parseFromConfig();
    }
    return parseFromSource(src);
  }

  /**
   * Parse form the configuration source.
   * @param src defines how the configuration is to be located.
   * @return the config as a {@link TypedProperties} object.
   */
  private TypedProperties parseFromSource(final String src)
  {
    TypedProperties config = JPPFConfiguration.getProperties();
    int idx = src.indexOf('|');
    if (idx <= 0) return config;
    String type = src.substring(0, idx).trim();
    String path = src.substring(idx+1).trim();
    InputStream is = null;
    try
    {
      if ("classpath".equalsIgnoreCase(type)) is = getClass().getClassLoader().getResourceAsStream(path);
      else if ("url".equalsIgnoreCase(type)) is = new URL(path).openStream();
      else if ("file".equalsIgnoreCase(type)) is = new BufferedInputStream(new FileInputStream(path));
      else throw new IllegalArgumentException("wrong type '"  + type + "' for confguration source, should be one of ['classpath', 'url', 'file']");
      if (is == null) throw new IllegalArgumentException("configuration source '" + src + "' does not point to a valid JPPF configuration");
      config.load(is);
    }
    catch (Exception e)
    {
      log.error("Error while initializing the JPPF client configuration", e);
    }
    finally
    {
      if (is != null) StreamUtils.closeSilent(is);
    }
    return config;
  }

  /**
   * Parse form the configuration specified as a string.
   * @return the config as a {@link TypedProperties} object.
   */
  private TypedProperties parseFromConfig()
  {
    TypedProperties config = JPPFConfiguration.getProperties();
    try
    {
      StringReader reader = new StringReader(clientConfiguration);
      try
      {
        config.load(reader);
      }
      finally
      {
        StreamUtils.closeSilent(reader);
      }
    }
    catch(Exception e)
    {
      log.error("Error while initializing the JPPF client configuration", e);
    }
    if (log.isDebugEnabled()) log.debug("config properties: " + config);
    return config;
  }
}