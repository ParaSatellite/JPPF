/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.jmxremote.nio;

import org.jppf.jmxremote.JMXEnvHelper;
import org.jppf.jmxremote.message.*;
import org.jppf.nio.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.Logger;

/**
 * Writes all messages in the channel's context queue, if any, or keep writing the current message.
 * @author Laurent Cohen
 */
public class SendingMessageState extends JMXNioState {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggingUtils.getLogger(SendingMessageState.class, JMXEnvHelper.isAsyncLoggingEnabled());
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   *
   * @param server the server which handles the channels states and transitions.
   */
  public SendingMessageState(final JMXNioServer server) {
    super(server);
  }

  @Override
  public JMXTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    final JMXContext context = ((JMXChannelWrapper) channel).context;
    while (true) {
      if (context.getChannels().isClosed()) return null;
      if (context.getCurrentMessageWrapper() == null) {
        final MessageWrapper msg = context.pollJmxMessage();
        if (msg == null) return null;
        if (debugEnabled) log.debug("about to send message {} from context {}", msg, context);
        context.setCurrentMessageWrapper(msg);
        context.setMessage(msg.nioMessage);
      }
      if (context.writeMessage(channel)) {
        final MessageWrapper msg = context.getCurrentMessageWrapper();
        if (debugEnabled) log.debug("fully sent message {} from context {}", msg, context);
        context.setMessage(null);
        context.setCurrentMessageWrapper(null);
        if (msg.jmxMessage.getMessageType() == JMXMessageType.CLOSE) {
          if (debugEnabled) log.debug("handling CLOSE for context {}", context);
          context.getMessageHandler().messageSent(msg.jmxMessage);
          return null;
        }
      } else if (context.byteCount <= 0L) {
        return JMXTransition.TO_SENDING_MESSAGE;
      }
    }
  }
}
