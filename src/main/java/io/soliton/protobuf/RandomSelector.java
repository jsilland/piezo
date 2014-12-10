/**
 * Copyright 2014 Peter Foldes
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

import java.util.List;
import java.util.Random;

/**
 * Select clients from a list randomly.
 *
 * @author Peter Foldes (peter.foldes@gmail.com)
 */
public class RandomSelector implements SelectionPolicy {

  private Random random = new Random();

  @Override
  public Client select(List<Client> pool) {
    if (pool.isEmpty()) {
      return null;
    }
    return pool.get(random.nextInt(pool.size()));
  }
}
