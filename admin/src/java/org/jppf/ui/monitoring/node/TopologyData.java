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

package org.jppf.ui.monitoring.node;

import org.jppf.client.*;
import org.jppf.management.*;
import org.slf4j.*;

/**
 * Instances of this class represent the state of a node in the Topology panel tree.
 * @author Laurent Cohen
 */
public class TopologyData
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TopologyData.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The type of this object: driver or node.
   */
  private TopologyDataType type = null;
  /**
   * The status of the node.
   */
  private TopologyDataStatus status = TopologyDataStatus.UP;
  /**
   * A driver connection.
   */
  private JPPFClientConnection clientConnection = null;
  /**
   * Wrapper holding the connection to the JMX server on a driver or a node.
   */
  private JMXConnectionWrapper jmxWrapper = null;
  /**
   * Information on the JPPF node .
   */
  private JPPFManagementInfo nodeInformation = null;
  /**
   * Object describing the current state of a node.
   */
  private JPPFNodeState nodeState = null;
  /**
   * Determines whether the corresponding driver is collapsed in the visualization panel.
   */
  private boolean collapsed = false;

  /**
   * Initialize topology job data with the specified type.
   * @param type the type of this job data object as a <code>JobDataType</code> enum value.
   */
  protected TopologyData(final TopologyDataType type)
  {
    this.type = type;
  }

  /**
   * Initialize this topology data as a driver related object.
   * @param clientConnection a reference to the driver connection.
   */
  public TopologyData(final JPPFClientConnection clientConnection)
  {
    this(TopologyDataType.DRIVER);
    this.clientConnection = clientConnection;
    this.jmxWrapper = ((JPPFClientConnectionImpl) clientConnection).getJmxConnection();
  }

  /**
   * Initialize this topology data as holding information about a node.
   * @param nodeInformation information on the JPPF node.
   */
  public TopologyData(final JPPFManagementInfo nodeInformation)
  {
    this(TopologyDataType.NODE);
    this.nodeInformation = nodeInformation;
    this.nodeState = new JPPFNodeState();
    if (nodeInformation.isNode())
    {
      jmxWrapper = new JMXNodeConnectionWrapper(nodeInformation.getHost(), nodeInformation.getPort(), nodeInformation.isSecure());
      jmxWrapper.connect();
    }
    else jmxWrapper = new JMXDriverConnectionWrapper(nodeInformation.getHost(), nodeInformation.getPort(), nodeInformation.isSecure());
  }

  /**
   * Get the type of this job data object.
   * @return a <code>TopologyDataType</code> enum value.
   */
  public TopologyDataType getType()
  {
    return type;
  }

  /**
   * Get the wrapper holding the connection to the JMX server on a driver or node.
   * @return a <code>JMXDriverConnectionWrapper</code> instance.
   */
  public JMXConnectionWrapper getJmxWrapper()
  {
    return jmxWrapper;
  }

  /**
   * Set the wrapper holding the connection to the JMX server on a driver or node.
   * @param jmxWrapper a <code>JMXDriverConnectionWrapper</code> instance.
   */
  public void setJmxWrapper(final JMXConnectionWrapper jmxWrapper)
  {
    this.jmxWrapper = jmxWrapper;
  }

  /**
   * Get the information on a JPPF node.
   * @return a <code>NodeManagementInfo</code> instance.
   */
  public JPPFManagementInfo getNodeInformation()
  {
    return nodeInformation;
  }

  /**
   * Get a string representation of this object.
   * @return a string displaying the host and port of the underlying jmx connection.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return jmxWrapper.getId();
  }

  /**
   * Get the object describing the current state of a node.
   * @return a <code>JPPFNodeState</code> instance.
   */
  public JPPFNodeState getNodeState()
  {
    return nodeState;
  }

  /**
   * Set the object describing the current state of a node.
   * @param nodeState a <code>JPPFNodeState</code> instance.
   */
  public void setNodeState(final JPPFNodeState nodeState)
  {
    this.nodeState = nodeState;
  }

  /**
   * Get the driver connection.
   * @return a <code>JPPFClientConnection</code> instance.
   */
  public JPPFClientConnection getClientConnection()
  {
    return clientConnection;
  }

  /**
   * Set the driver connection.
   * @param clientConnection a <code>JPPFClientConnection</code> instance.
   */
  public void setClientConnection(final JPPFClientConnection clientConnection)
  {
    this.clientConnection = clientConnection;
  }

  /**
   * Refresh the state of the node represented by this topology data.
   */
  public void refreshNodeState()
  {
    if (!TopologyDataType.NODE.equals(type)) return;
    try
    {
      if (!jmxWrapper.isConnected()) return;
      if (jmxWrapper instanceof JMXNodeConnectionWrapper)
      {
        nodeState = ((JMXNodeConnectionWrapper) jmxWrapper).state();
        if (nodeState == null) setStatus(TopologyDataStatus.DOWN);
        else setStatus(TopologyDataStatus.UP);
      }
    }
    catch(Exception e)
    {
      setStatus(TopologyDataStatus.DOWN);
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
  }

  /**
   * Get the status of the node.
   * @return the node status.
   */
  public TopologyDataStatus getStatus()
  {
    return status;
  }

  /**
   * Set the status of the node.
   * @param status the node status.
   */
  public void setStatus(final TopologyDataStatus status)
  {
    this.status = status;
  }

  /**
   * Get the id of this topology element.
   * @return the id as a string.
   */
  public String getId()
  {
    return (jmxWrapper == null) ? null : jmxWrapper.getId();
  }

  /**
   * Determine whether this object represents an node.
   * @return <code>true</code> if this object represets a node, <code>false</code> otherwise.
   */
  public boolean isNode()
  {
    return (type == TopologyDataType.NODE) && (nodeInformation != null) && nodeInformation.isNode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    TopologyData other = (TopologyData) obj;
    if (getId() == null)
    {
      if (other.getId() != null) return false;
    }
    else if (!getId().equals(other.getId())) return false;
    return true;
  }

  /**
   * Determine whether the corresponding driver is collapsed in the visualization panel.
   * @return <code>true</code> if the driver is collapsed, <code>false</code> otherwise.
   */
  public boolean isCollapsed()
  {
    return collapsed;
  }

  /**
   * Specify whether the corresponding driver is collapsed in the visualization panel.
   * @param collapsed <code>true</code> if the driver is collapsed, <code>false</code> otherwise.
   */
  public void setCollapsed(final boolean collapsed)
  {
    this.collapsed = collapsed;
  }
}
