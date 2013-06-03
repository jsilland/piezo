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

import com.google.protobuf.Parser;

/**
 * Represents a method as seen by the client-side of the RPC system.
 *
 * @author Julien Silland (julien@soliton.io)
 * @param <O> the return type of the method.
 */
public interface ClientMethod<O> {

  /**
   * Returns the name of the service in which this method is defined.
   */
  public String serviceName();

  /**
   * Returns the name of this method.
   */
  public String name();

  /**
   * Returns a parser capable of handling content for this method's return
   * type.
   */
  public Parser<O> outputParser();
}
