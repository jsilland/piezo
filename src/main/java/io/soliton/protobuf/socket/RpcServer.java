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

import io.soliton.protobuf.AbstractRpcServer;
import io.soliton.protobuf.ChannelInitializers;
import io.soliton.protobuf.Envelope;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Simple implementation of {@link io.soliton.protobuf.Server} using a TCP transport.
 * <p/>
 * <p>This server uses a proprietary binary protocol to communicate with
 * instances of {@link RpcClient}.</p>
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class RpcServer extends AbstractRpcServer {

  /**
   * Creates a new server configure to bind to the given TCP port.
   *
   * @param port the TCP port to bind to
   */
  public RpcServer(int port) {
    super(port, NioServerSocketChannel.class, new NioEventLoopGroup(), new NioEventLoopGroup());
  }

  protected ChannelInitializer<? extends Channel> channelInitializer() {
    return ChannelInitializers.protoBuf(Envelope.getDefaultInstance(),
        new RpcServerHandler(serviceGroup()));
  }
}
