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

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

import static org.mockito.Mockito.mock;

/**
 * @author Peter Foldes (peter.foldes@gmail.com)
 */
public class ClientPoolTest {

  @Test
  public void testNullPool() {
    ClientPool pool = new ClientPool(new RoundRobinSelector());
    Assert.assertNull(pool.nextClient());
  }

  @Test
  public void testRoundRobinSelector() {
    Client client1 = mock(Client.class);
    Client client2 = mock(Client.class);

    ClientPool pool = new ClientPool(new RoundRobinSelector());
    pool.add(client1);
    pool.add(client2);

    Assert.assertEquals(client1, pool.nextClient());
    Assert.assertEquals(client2, pool.nextClient());
    Assert.assertEquals(client1, pool.nextClient());
  }

  @Test
  public void testRandomSelector() {
    Client client1 = mock(Client.class);
    Client client2 = mock(Client.class);
    Set<Client> clientSet = Sets.newHashSet(client1, client2);

    ClientPool pool = new ClientPool(new RoundRobinSelector());
    pool.addAll(clientSet);

    Assert.assertTrue(clientSet.contains(pool.nextClient()));
    Assert.assertTrue(clientSet.contains(pool.nextClient()));
    Assert.assertTrue(clientSet.contains(pool.nextClient()));
  }
}
