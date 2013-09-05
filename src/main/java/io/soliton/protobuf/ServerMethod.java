/**
 * Copyright 2013 Julien Silland
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.soliton.protobuf;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

/**
 * Represents a method on the server side of the RPC system.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public interface ServerMethod<I, O> {

  /**
   * Returns the name of this method.
   */
  public String name();

  /**
   * Returns a parser capable of parsing this method's input parameter.
   */
  public Parser<I> inputParser();

  /**
   * Returns a builder capable of building this method's input parameter.
   */
  public Message.Builder inputBuilder();

  /**
   * Invokes this method with the given parameter.
   *
   * @param request the parameter received from the client
   */
  public ListenableFuture<O> invoke(I request);
}
