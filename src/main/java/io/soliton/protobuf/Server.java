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

/**
 * High-level formalization of the server-side of the RPC system.
 * <p/>
 * <p>A server maintains a single {@link ServiceGroup} in which concrete
 * {@link Service} instances are kept. Server instances are responsible for
 * decoding method calls that are sent by a {@link Client} instance. As such,
 * concrete implementations of clients and servers are generally coupled and
 * must use the same protocol and transport.</p>
 * <p/>
 * <p>The configuration, startup and shutdown sequences of a server are
 * delegated to implementations of this abstraction.</p>
 *
 * @author Julien Silland (julien@soliton.io)
 */
public interface Server {

  /**
   * Returns the service group in which the services hosted by this server
   * are kept.
   */
  public ServiceGroup serviceGroup();
}
