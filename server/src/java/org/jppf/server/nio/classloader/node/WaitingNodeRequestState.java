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

package org.jppf.server.nio.classloader.node;

import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.util.*;

import org.jppf.classloader.*;
import org.jppf.server.nio.*;
import org.jppf.server.nio.classloader.*;
import org.jppf.server.nio.classloader.client.ClientClassNioServer;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class represents the state of waiting for a request from a node.
 * @author Laurent Cohen
 */
class WaitingNodeRequestState extends ClassServerState
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(WaitingNodeRequestState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The class cache.
   */
  private static ClassCache classCache = driver.getInitializer().getClassCache();

  /**
   * Initialize this state with a specified NioServer.
   * @param server the JPPFNIOServer this state relates to.
   */
  public WaitingNodeRequestState(final ClassNioServer server)
  {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
   */
  @Override
  public ClassTransition performTransition(final ChannelWrapper<?> channel) throws Exception
  {
    ClassContext context = (ClassContext) channel.getContext();
    if (context.readMessage(channel))
    {
      CompositeResourceWrapper composite = (CompositeResourceWrapper) context.deserializeResource();
      if (debugEnabled) log.debug("read resource request " + composite + " from node: " + channel);
      Collection<JPPFResourceWrapper> requests = composite.getResources();
      for (JPPFResourceWrapper resource: requests)
      {
        TraversalList<String> uuidPath = resource.getUuidPath();
        boolean dynamic = resource.isDynamic();
        String name = resource.getName();
        String uuid = (uuidPath.size() > 0) ? uuidPath.getCurrentElement() : null;
        ClassTransition t = null;
        if (resource.getCallable() != null)
        {
          boolean breakpoint = true;
        }
        if (!dynamic || (resource.getRequestUuid() == null)) t = processNonDynamic(channel, resource);
        else t = processDynamic(channel, resource);
        //if (t == TO_NODE_WAITING_PROVIDER_RESPONSE) context.getPendingResponses().put(resource, new ResourceRequest(channel, resource));
        //if (debugEnabled) log.debug("resource [" + name + "] not found for node: " + channel);
        //if (p.first() != null) resource.setDefinition(p.first());
      }
      if (context.getPendingResponses().isEmpty())
      {
        if (debugEnabled) log.debug("sending response " + composite + " to node: " + channel);
        context.serializeResource();
        return TO_SENDING_NODE_RESPONSE;
      }
      if (debugEnabled) log.debug("pending responses " + context.getPendingResponses() + " for node: " + channel);
      //return TO_NODE_WAITING_PROVIDER_RESPONSE;
      return TO_IDLE_NODE;
    }
    return TO_WAITING_NODE_REQUEST;
  }

  /**
   * Process a request to the driver's resource provider.
   * @param channel encapsulates the context and channel.
   * @param resource the resource request description
   * @return a pair of an array of bytes and the resulting state transition.
   * @throws Exception if any error occurs.
   */
  private ClassTransition processNonDynamic(final ChannelWrapper<?> channel, final JPPFResourceWrapper resource) throws Exception
  {
    byte[] b = null;
    String name = resource.getName();
    ClassContext context = (ClassContext) channel.getContext();
    TraversalList<String> uuidPath = resource.getUuidPath();

    String uuid = (uuidPath.size() > 0) ? uuidPath.getCurrentElement() : null;
    if (((uuid == null) || uuid.equals(driver.getUuid())) && (resource.getCallable() == null))
    {
      if (resource.getData("multiple") != null)
      {
        List<byte[]> list = server.getResourceProvider().getMultipleResourcesAsBytes(name, null);
        if (debugEnabled) log.debug("multiple resources " + (list != null ? "" : "not ") + "found [" + name + "] in driver's classpath for node: " + channel);
        if (list != null)
        {
          resource.setData("resource_list", list);
          return TO_SENDING_NODE_RESPONSE;
        }
      }
      else if (resource.getData("multiple.resources.names") != null)
      {
        String[] names = (String[]) resource.getData("multiple.resources.names");
        Map<String, List<byte[]>> map = server.getResourceProvider().getMultipleResourcesAsBytes(null, names);
        resource.setData("resource_map", map);
        return TO_SENDING_NODE_RESPONSE;
      }
      else
      {
        if ((uuid == null) && !resource.isDynamic()) uuid = driver.getUuid();
        if (uuid != null) b = classCache.getCacheContent(uuid, name);
        boolean alreadyInCache = (b != null);
        if (debugEnabled) log.debug("resource " + (alreadyInCache ? "" : "not ") + "found [" + name + "] in cache for node: " + channel);
        if (!alreadyInCache)
        {
          b = server.getResourceProvider().getResourceAsBytes(name);
          if (debugEnabled) log.debug("resource " + (b == null ? "not " : "") + "found [" + name + "] in the driver's classpath for node: " + channel);
        }
        if ((b != null) || !resource.isDynamic())
        {
          if ((b != null) && !alreadyInCache) classCache.setCacheContent(driver.getUuid(), name, b);
          resource.setDefinition(b);
          return TO_SENDING_NODE_RESPONSE;
        }
      }
    }
    resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
    return null;
  }

  /**
   * Process a request to the client's resource provider.
   * @param channel encapsulates the context and channel.
   * @param resource the resource request description
   * @return the resulting state transition.
   * @throws Exception if any error occurs.
   */
  private ClassTransition processDynamic(final ChannelWrapper<?> channel, final JPPFResourceWrapper resource) throws Exception
  {
    byte[] b = null;
    String name = resource.getName();
    TraversalList<String> uuidPath = resource.getUuidPath();
    ClassContext context = (ClassContext) channel.getContext();
    if (resource.getCallable() == null) b = classCache.getCacheContent(uuidPath.getFirst(), name);
    if (b != null)
    {
      if (debugEnabled) log.debug("found cached resource [" + name + "] for node: " + channel);
      resource.setDefinition(b);
      return TO_SENDING_NODE_RESPONSE;
    }
    uuidPath.decPosition();
    String uuid = resource.getUuidPath().getCurrentElement();
    ChannelWrapper<?> provider = findProviderConnection(uuid);
    if (provider != null)
    {
      if (debugEnabled) log.debug("requesting resource " + resource + " from client: " + provider + " for node: " + channel);
      ClassContext providerContext = (ClassContext) provider.getContext();
      synchronized(provider)
      {
        ResourceRequest request = new ResourceRequest(channel, resource);
        context.getPendingResponses().put(resource, request);
        providerContext.addRequest(request);
        resource.setState(JPPFResourceWrapper.State.PROVIDER_REQUEST);
      }
    }
    else
    {
      if (debugEnabled) log.debug("no available provider for uuid=" + uuid + " : setting null response for node " + channel);
      resource.setDefinition(null);
      resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
    }
		return TO_NODE_WAITING_PROVIDER_RESPONSE;
  }

  /**
   * Find a provider connection for the specified provider uuid.
   * @param uuid the uuid for which to find a connection.
   * @return a <code>SelectableChannel</code> instance.
   * @throws Exception if an error occurs while searching for a connection.
   */
  private ChannelWrapper<?> findProviderConnection(final String uuid) throws Exception
  {
    ChannelWrapper<?> result = null;
    ClientClassNioServer clientClassServer = (ClientClassNioServer) driver.getClientClassServer();
    List<ChannelWrapper<?>> connections = clientClassServer.getProviderConnections(uuid);
    if (connections == null) return null;
    int minRequests = Integer.MAX_VALUE;
    for (ChannelWrapper<?> channel: connections)
    {
      ClassContext ctx = (ClassContext) channel.getContext();
      int size = ctx.getNbPendingRequests();
      if (size < minRequests)
      {
        minRequests = size;
        result = channel;
      }
    }
    return result;
  }
}
