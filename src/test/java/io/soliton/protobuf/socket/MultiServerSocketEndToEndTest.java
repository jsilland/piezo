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

package io.soliton.protobuf.socket;

import io.soliton.protobuf.AbstractEndToEndTest;
import io.soliton.protobuf.Client;
import io.soliton.protobuf.ClientPool;
import io.soliton.protobuf.RandomSelector;
import io.soliton.protobuf.Server;

import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.List;

/**
 * End-to-end tests for multiple servers and clients
 *
 * @author Peter Foldes (peter.foldes@gmail.com)
 */
public class MultiServerSocketEndToEndTest extends AbstractEndToEndTest {

  private static List<RpcServer> servers = Lists.newArrayList();

  @BeforeClass
  public static void setUp() throws Exception {
    for (int i = 0; i < 2; i++) {
      RpcServer server = RpcServer.newServer(findAvailablePort()).build();
      server.startAsync().awaitRunning();
      servers.add(server);
    }
  }

  @AfterClass
  public static void tearDown() {
    for (RpcServer server : servers) {
      server.stopAsync().awaitTerminated();
    }
  }

  @Override
  protected List<? extends Server> servers() {
    return this.servers;
  }

  @Override
  protected Client client() throws Exception {
    ClientPool pool = new ClientPool(new RandomSelector());
    for (RpcServer server : this.servers) {
      pool.add(RpcClient
          .newClient(HostAndPort.fromParts("localhost", server.getPort()))
          .build());
    }
    return pool;
  }
}
