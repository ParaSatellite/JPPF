/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.nio.acceptor;

import java.nio.channels.*;

import org.jppf.nio.*;
import org.jppf.serialization.SerializationUtils;
import org.jppf.utils.*;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * Context associated with a channel serving tasks to a node.
 * @author Laurent Cohen
 */
public class AcceptorContext extends SimpleNioContext<EmptyEnum> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AcceptorContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Identifier for the channel.
   */
  private int id = JPPFIdentifiers.UNKNOWN;
  /**
   * Contains the data read from the socket channel.
   */
  private NioObject nioObject = null;
  /**
   * The acceptor server.
   */
  final AcceptorNioServer server;
  /**
   * The statsistics to update, if any.
   */
  private final JPPFStatistics stats;
  /**
   * The server socket channel that accepted the connection.
   */
  private final ServerSocketChannel serverSocketChannel;
  /**
   * The socket channel's interest ops.
   */
  private int interestOps;

  /**
   * 
   * @param server the acceptor server.
   * @param serverSocketChannel the server socket channel that accepted the connection.
   * @param socketChannel the socket channel that represent the connection.
   * @param stats the statsistics to update, if any.
   */
  public AcceptorContext(final AcceptorNioServer server, final ServerSocketChannel serverSocketChannel, final SocketChannel socketChannel, final JPPFStatistics stats) {
    this.server = server;
    this.stats = stats;
    this.serverSocketChannel = serverSocketChannel;
    this.socketChannel = socketChannel;
  }

  /**
   * Read data from a channel. This method reads a single integer which identifies the type of the channel.
   * @param wrapper the channel to read the data from.
   * @return true if all the data has been read, false otherwise.
   * @throws Exception if an error occurs while reading the data.
   * @see org.jppf.utils.JPPFIdentifiers
   */
  @Override
  public boolean readMessage(final ChannelWrapper<?> wrapper) throws Exception {
    if (nioObject == null) nioObject = new PlainNioObject(socketChannel, 4);
    boolean b = false;
    try {
      byteCount = nioObject.getChannelCount();
      b = nioObject.read();
      byteCount = nioObject.getChannelCount() - byteCount;
      if (debugEnabled) log.debug("read {} bytes from {}", byteCount, this);
    } catch (final Exception e) {
      if (stats != null) stats.addValue(JPPFStatisticsHelper.JMX_IN_TRAFFIC, nioObject.getChannelCount());
      throw e;
    }
    if (b) {
      id = SerializationUtils.readInt(nioObject.getData().getInputStream());
      if (stats != null) stats.addValue(JPPFStatisticsHelper.JMX_IN_TRAFFIC, nioObject.getChannelCount());
      nioObject = null;
    }
    return b;
  }

  @Override
  public void handleException(final ChannelWrapper<?> channel, final Exception e) {
  }

  /**
   * get the identifier for the channel.
   * @return the identifier as an int value.
   */
  public int getId() {
    return id;
  }

  /**
   * @return the server socket channel that accepted the connection.
   */
  public ServerSocketChannel getServerSocketChannel() {
    return serverSocketChannel;
  }

  /**
   * @return the socket channel's interest ops.
   */
  public int getInterestOps() {
    return interestOps;
  }

  /**
   * Set the socket channel's interest ops.
   * @param interestOps the interest ops to set.
   */
  public void setInterestOps(final int interestOps) {
    this.interestOps = interestOps;
  }
}
