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

package io.soliton.protobuf.quartz;

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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.GenericFutureListener;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * An RPC client which encodes method calls in {@link Envelope} messages
 * and uses an HTTP transport.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class QuartzClient implements Client {

  private static final Logger logger = Logger.getLogger(QuartzClient.class.getCanonicalName());

  private final Channel channel;
  private final QuartzClientHandler handler;
  private final ClientLogger clientLogger;

  public static Builder newClient(HostAndPort remoteAddress) {
    return new Builder(remoteAddress);
  }

  /**
   * Protected exhaustive constructor
   *
   * @param channel the channel to the remote server
   * @param handler the client-side handler in charge of handling responses
   */
  QuartzClient(Channel channel, QuartzClientHandler handler, ClientLogger clientLogger) {
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
    final EnvelopeFuture<O> output = handler.newProvisionalResponse(method.outputParser());
    Envelope request = Envelope.newBuilder()
        .setRequestId(output.requestId())
        .setService(method.serviceName())
        .setMethod(method.name())
        .setPayload(input.toByteString())
        .build();

    HttpRequest httpRequest = handler.convertRequest(request);
    channel.writeAndFlush(httpRequest).addListener(new GenericFutureListener<ChannelFuture>() {

      public void operationComplete(ChannelFuture future) {
        if (!future.isSuccess()) {
          clientLogger.logClientError(method, future.cause());
          handler.finish(output.requestId());
          output.setException(future.cause());
        }
      }

    });

    return output;
  }

  /**
   * Configurable builder for instances of {@link QuartzClient}.
   */
  public static final class Builder {

    private final HostAndPort remoteAddress;
    private SSLContext sslContext;
    private String path = "/quartz";
    private ClientLogger clientLogger = new NullClientLogger();

    private Builder(HostAndPort remoteAddress) {
      this.remoteAddress = Preconditions.checkNotNull(remoteAddress);
    }

    /**
     * Sets the URL path of the remote server.
     *
     * @param path the remote {@link QuartzServer} endpoint path
     * @return {@code this} object
     */
    public Builder setPath(String path) {
      Preconditions.checkNotNull(path);
      Preconditions.checkArgument(path.startsWith("/"));
      this.path = path;
      return this;
    }

    /**
     * Secures the communication with the remote server using the given SSL
     * configuration.
     *
     * @param sslContext the SSL configuration this client should follow
     * @return {@code this} object
     */
    public Builder setSslContext(SSLContext sslContext) {
      this.sslContext = Preconditions.checkNotNull(sslContext);
      return this;
    }

    /**
     * Sets the monitoring hook to be used by the client.
     *
     * @param clientLogger a monitoring logger
     * @return {@code this} object
     */
    public Builder setClientLogger(ClientLogger clientLogger) {
      this.clientLogger = Preconditions.checkNotNull(clientLogger);
      return this;
    }

    /**
     * Returns a new connected {@link QuartzClient} based on the configuration
     * of this builder.
     *
     * @throws IOException if the client failed to connect to the server
     */
    public QuartzClient build() throws IOException {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(new NioEventLoopGroup());
      bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
      bootstrap.channel(NioSocketChannel.class);
      QuartzClientHandler handler = new QuartzClientHandler();
      ChannelInitializer<Channel> channelInitializer = sslContext == null ?
          ChannelInitializers.httpClient(handler) :
          ChannelInitializers.secureHttpClient(handler, sslContext);
      bootstrap.handler(channelInitializer);

      ChannelFuture future = bootstrap.connect(remoteAddress.getHostText(), remoteAddress.getPort());
      future.awaitUninterruptibly();
      if (future.isSuccess()) {
        logger.info("Piezo client successfully connected to " + remoteAddress.toString());
      } else {
        logger.warning("Piezo client failed to connect to " + remoteAddress.toString());
        throw new IOException(future.cause());
      }
      return new QuartzClient(future.channel(), handler, clientLogger);
    }
  }
}
