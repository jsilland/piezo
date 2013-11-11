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
 * A container of {@link Service} instances, used by a {@link Server} to
 * maintain the set of active services.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public interface ServiceGroup {

  /**
   * Adds a service to this group.
   * <p/>
   * <p>A service previously registered under the same
   * {@link Service#fullName()} will be replaced by the newer one.</p>
   *
   * @param service the service to add.
   */
  public void addService(Service service);

  /**
   * Looks up a service in this group by its full name.
   *
   * @param name the full name of the service to look up.
   * @return a {@link Service} instance, or {@code null} if no such service
   *         exists in this group.
   */
  public Service lookupByName(String name);
}
