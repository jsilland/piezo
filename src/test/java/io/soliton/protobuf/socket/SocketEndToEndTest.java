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

package io.soliton.protobuf.socket;

import io.soliton.protobuf.AbstractEndToEndTest;
import io.soliton.protobuf.Client;
import io.soliton.protobuf.Server;

import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.List;

/**
 * End-to-end tests for the socket-based client-server pair.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class SocketEndToEndTest extends AbstractEndToEndTest {

  private static RpcServer server;

  @BeforeClass
  public static void setUp() throws Exception {
    server = RpcServer.newServer(findAvailablePort()).build();
    server.startAsync().awaitRunning();
  }

  @AfterClass
  public static void tearDown() {
    server.stopAsync().awaitTerminated();
  }

  @Override
  protected List<? extends Server> servers() {
    return Lists.newArrayList(server);
  }

  @Override
  protected Client client() throws IOException {
    return RpcClient.newClient(HostAndPort.fromParts("localhost", server.getPort())).build();
  }
}
