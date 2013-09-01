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

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;

/**
 * Represents an RPC service as defined in a {@code .proto} file.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public interface Service {

  /**
   * Looks up a method in this service.
   *
   * @param name the name of the method to look up
   * @return the found method of {@code null} if no method with the given
   * name was found
   */
  public ServerMethod<? extends Message, ? extends Message> lookup(String name);

  /**
   * Returns the set of methods defined in this service, keyed by name.
   */
  public ImmutableMap<String, ServerMethod<? extends Message, ? extends Message>> methods();

  /**
   * Returns the name of the service, as declared in the {@code .proto} file.
   */
  public String shortName();

  /**
   * Returns the fully-qualified name of the service, which includes the
   * {@code .proto} package name.
   */
  public String fullName();
}
