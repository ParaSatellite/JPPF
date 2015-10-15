/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
package sample.helloworld;

import java.io.Serializable;

import org.jppf.node.protocol.JPPFRunnable;

/**
 * A simple hello world JPPF task with a JPPF-annotated instance method.
 * @author Laurent Cohen
 */
public class HelloWorldAnnotated implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Execute the task.
   * @param message a message to print.
   * @param number an example primitive argument.
   * @return a string hello message.
   */
  @JPPFRunnable
  public String helloMethod(final String message, final int number)
  {
    String hello = "Hello, World (annotated, " + message + ", " + number + ')';
    System.out.println(hello);
    return hello;
  }
}
