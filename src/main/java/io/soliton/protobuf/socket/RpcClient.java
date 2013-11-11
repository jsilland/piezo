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

import io.soliton.protobuf.ChannelInitializers;
import io.soliton.protobuf.Client;
import io.soliton.protobuf.ClientLogger;
import io.soliton.protobuf.ClientMethod;
import io.soliton.protobuf.Envelope;
import io.soliton.protobuf.EnvelopeFuture;
import io.soliton.protobuf.NullClientLogger;

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

import java.io.IOException;
import java.util.logging.Logger;

/**
 * A simple client implementation connecting to a remote server over a
 * TCP socket.
 * <p/>
 * <p>This client uses a proprietary binary protocol to communicate with
 * an instance of {@link RpcServer}.</p>
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class RpcClient implements Client {

  private static final Logger logger = Logger.getLogger(
      RpcClient.class.getCanonicalName());

  private final Channel channel;
  private final RpcClientHandler handler;
  private final ClientLogger clientLogger;

  /**
   * Returns a new configurable builder of {@link RpcClient} instances.
   *
   * @param remoteAddress the address of the remote server to connect to
   */
  public static Builder newClient(HostAndPort remoteAddress) {
    return new Builder(remoteAddress);
  }

  /**
   * Exhaustive constructor.
   *
   * @param channel the connected channel to the remote server
   * @param handler the handler in charge of decoding and dispatching server
   * responses
   * @param clientLogger the monitoring logger to which events should be
   * reported
   */
  RpcClient(Channel channel, RpcClientHandler handler, ClientLogger clientLogger) {
    this.channel = channel;
    this.handler = handler;
    this.clientLogger = clientLogger;
    handler.setChannel(channel);
    handler.setClientLogger(clientLogger);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <O extends Message> ListenableFuture<O> encodeMethodCall(final ClientMethod<O> method,
      Message input) {
    clientLogger.logMethodCall(method);
    final EnvelopeFuture<O> output = handler.newProvisionalResponse(method);
    Envelope request = Envelope.newBuilder()
        .setRequestId(output.requestId())
        .setService(method.serviceName())
        .setMethod(method.name())
        .setPayload(input.toByteString())
        .build();
    // TODO(julien): might be nice to couple the future returned from writeAndFlush
    // into the one returned to the user, so that calling cancel on the userland
    // future may also cancel the outgoing request if it isn't done yet.
    channel.writeAndFlush(request).addListener(new GenericFutureListener<ChannelFuture>() {

      public void operationComplete(ChannelFuture future) {
        if (!future.isSuccess()) {
          clientLogger.logLinkError(method, future.cause());
          handler.finish(output.requestId());
          output.setException(future.cause());
        }
      }

    });
    return output;
  }

  public static final class Builder {

    private final HostAndPort remoteAddress;
    private ClientLogger clientLogger = new NullClientLogger();

    private Builder(HostAndPort remoteAddress) {
      this.remoteAddress = Preconditions.checkNotNull(remoteAddress);
    }

    /**
     * Sets the logger to which monitorable events should be reported.
     *
     * @return {@code this} object
     */
    public Builder setClientLogger(ClientLogger clientLogger) {
      this.clientLogger = Preconditions.checkNotNull(clientLogger);
      return this;
    }

    /**
     * Returns a new connected {@link RpcClient}, as configured by this object
     *
     * @throws IOException in case of a connection error.
     */
    public RpcClient build() throws IOException {
      Preconditions.checkNotNull(remoteAddress);
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(new NioEventLoopGroup());
      bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
      bootstrap.channel(NioSocketChannel.class);
      RpcClientHandler handler = new RpcClientHandler();
      bootstrap.handler(ChannelInitializers.protoBuf(Envelope.getDefaultInstance(), handler));

      ChannelFuture future = bootstrap.connect(remoteAddress.getHostText(), remoteAddress.getPort());
      future.awaitUninterruptibly();
      if (future.isSuccess()) {
        logger.info("Piezo client successfully connected to " + remoteAddress.toString());
      } else {
        logger.warning("Piezo client failed to connect to " + remoteAddress.toString());
        throw new IOException(future.cause());
      }
      return new RpcClient(future.channel(), handler, clientLogger);
    }
  }
}
