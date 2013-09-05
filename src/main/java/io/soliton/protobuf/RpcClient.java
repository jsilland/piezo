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

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.logging.Logger;

/**
 * A simple client implementation connecting to a remote server over a
 * TCP link.
 *
 * <p>This client uses a proprietary binary protocol to communicate with
 * an instance of {@link RpcServer}.</p>
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class RpcClient implements Client {

  private static final Logger logger = Logger.getLogger(
      RpcClient.class.getCanonicalName());

  private final Channel channel;
  private final RpcClientHandler handler = new RpcClientHandler();

  /**
   * Creates a new, single transport connected to the given remote host.
   *
   * @param remoteAddress the coordinates of the remote host to connect to
   */
  public RpcClient(HostAndPort remoteAddress) {
    Preconditions.checkNotNull(remoteAddress);
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(new NioEventLoopGroup());
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
    bootstrap.channel(NioSocketChannel.class);
    bootstrap.handler(ChannelInitializers.protoBuf(Envelope.getDefaultInstance(), handler));

    ChannelFuture future = bootstrap.connect(remoteAddress.getHostText(), remoteAddress.getPort());
    future.awaitUninterruptibly();
    if (future.isSuccess()) {
      logger.info("Piezo client successfully connected to " + remoteAddress.toString());
      this.channel = future.channel();
      handler.setChannel(this.channel);
    } else {
      logger.warning("Piezo client failed to connect to " + remoteAddress.toString());
      throw new RuntimeException(future.cause());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <O extends Message> ListenableFuture<O> encodeMethodCall(
      ClientMethod<O> method, Message input) {
    final ResponseFuture<O> output = handler.newProvisionalResponse(method.outputParser());
    Envelope request = Envelope.newBuilder()
        .setRequestId(output.requestId())
        .setService(method.serviceName())
        .setMethod(method.name())
        .setPayload(input.toByteString())
        .build();
    channel.writeAndFlush(request).addListener(new GenericFutureListener<ChannelFuture>() {

      public void operationComplete(ChannelFuture future) {
        if (!future.isSuccess()) {
          handler.finish(output.requestId());
          output.setException(future.cause());
        }
      }

    });
    return output;
  }
}
