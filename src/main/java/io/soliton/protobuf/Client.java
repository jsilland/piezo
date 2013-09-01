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

/**
 * High-level formalization of the client-side of the RPC system.
 *
 * <p>A client's responsibility is to encode a method call and send it to a
 * remote {@link Server} instance. The protocol and transport used to
 * accomplish this task are specific to each client-server implementation.</p>
 *
 * @author Julien Silland (julien@soliton.io)
 */
public interface Client {

  /**
   * Encodes a single method call and propagates it to the service end of this
   * transport.
   *
   * @param method the method being called
   * @param input the method's parameter
   * @return a handle on the eventual response received from the service
   */
  public <O extends Message> ListenableFuture<O> encodeMethodCall(
      ClientMethod<O> method, Message input);
}
