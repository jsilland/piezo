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

package io.soliton.protobuf.json;

import io.soliton.protobuf.AbstractRpcServer;
import io.soliton.protobuf.ChannelInitializers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Concrete {@link io.soliton.protobuf.Server} implementation surfacing the
 * registered services over an HTTP transport using the JSON-RPC protocol.
 *
 * @author Julien Silland (julien@soliton.io)
 * @see <a href="http://json-rpc.org/">JSON-RPC</a>
 */
public class HttpJsonRpcServer extends AbstractRpcServer {

  private final String rpcPath;

  /**
   * Exhaustive constructor.
   *
   * @param port the TCP port this server should bind to
   * @param rpcPath the URL path to which the JSON-RPC handler should be bound
   */
  public HttpJsonRpcServer(int port, String rpcPath) {
    super(port, NioServerSocketChannel.class, new NioEventLoopGroup(), new NioEventLoopGroup());
    this.rpcPath = rpcPath;
  }

  @Override
  protected ChannelInitializer<? extends Channel> channelInitializer() {
    return ChannelInitializers.httpServer(new JsonRpcServerHandler(this, rpcPath));
  }

  public static void main(String... args) throws Exception {
    HttpJsonRpcServer server = new HttpJsonRpcServer(3000, "rpc");
    server.start();
  }
}
