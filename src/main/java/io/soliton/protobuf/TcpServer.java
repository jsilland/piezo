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
import com.google.common.base.Throwables;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Concrete implementation of a {@link Server} binding to a TCP port.
 *
 * <p>This implementation uses a proprietary protocol for encoding method
 * calls.</p>
 *
 * @author Julien Silland (julien@soliton.io)
 */
public final class TcpServer extends SimpleChannelInboundHandler<Envelope> implements Server {

  private static final Logger logger = Logger.getLogger(
      TcpServer.class.getCanonicalName());

  private final int port;
  private final ServiceGroup serviceGroup;
  private final ConcurrentMap<Long, ListenableFuture<?>> pendingRequests;

  private Channel channel;
  private EventLoopGroup parentGroup;
  private EventLoopGroup childGroup;

  /**
   * Creates a new server.
   *
   * @param port the port the server should bind to
   */
  public TcpServer(int port) {
    Preconditions.checkArgument(port > 0);
    this.port = port;
    serviceGroup = new DefaultServiceGroup();
    pendingRequests = new MapMaker().makeMap();
  }

  /**
   * Returns the set of services surfaced on this server.
   */
  public ServiceGroup serviceGroup() {
    return serviceGroup;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSharable() {
    return true;
  }

  /**
   * Starts this server.
   *
   * <p>This is a synchronous operation.</p>
   */
  public void start() {
    logger.info(String.format("Starting Piezo server on TCP port %d", port));
    ServerBootstrap bootstrap = new ServerBootstrap();
    parentGroup = new NioEventLoopGroup();
    childGroup = new NioEventLoopGroup();
    bootstrap.group(parentGroup, childGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<Channel>() {

          @Override
          protected void initChannel(Channel channel) throws Exception {
            channel.pipeline().addLast("frameDecoder",
                new LengthFieldBasedFrameDecoder(10 * 1024 * 1024, 0, 4, 0, 4));
            channel.pipeline().addLast("protobufDecoder",
                new ProtobufDecoder(Envelope.getDefaultInstance()));
            channel.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
            channel.pipeline().addLast("protobufEncoder", new ProtobufEncoder());
            channel.pipeline().addLast("piezoServerTransport", TcpServer.this);

          }
        });

    ChannelFuture futureChannel = bootstrap.bind(port).awaitUninterruptibly();
    if (futureChannel.isSuccess()) {
      this.channel = futureChannel.channel();
      logger.info("Piezo server started successfully.");
    } else {
      logger.info("Failed to start Piezo server.");
      throw new RuntimeException(futureChannel.cause());
    }
  }

  /**
   * Stops this server.
   *
   * <p>This is a synchronous operation.</p>
   */
  public void stop() {
    logger.info("Shutting down Piezo server.");
    channel.close().addListener(new GenericFutureListener<Future<Void>>() {

      @Override
      public void operationComplete(Future<Void> future) throws Exception {
        parentGroup.shutdownGracefully();
        childGroup.shutdownGracefully();
      }
    }).awaitUninterruptibly();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelRead0(ChannelHandlerContext context, Envelope request)
      throws Exception {
    if (request.hasControl() && request.getControl().getCancel()) {
      ListenableFuture<?> pending = pendingRequests.remove(request.getRequestId());
      if (pending != null) {
        boolean cancelled = pending.cancel(true);
        channel.write(Envelope.newBuilder()
            .setRequestId(request.getRequestId())
            .setControl(Control.newBuilder().setCancel(cancelled)).build());
      }
    }

    Service service = serviceGroup.lookupByName(request.getService());
    if (service == null) {
      logger.warning(String.format(
          "Received request for unknown service %s", request.getService()));
      context.channel().write(Envelope.newBuilder()
          .setRequestId(request.getRequestId())
          .setControl(Control.newBuilder()
              .setError(String.format("Unknown service %s", request.getService())))
          .build());
    }

    ServerMethod<? extends Message, ? extends Message> method =
        service.lookup(request.getMethod());
    if (method == null) {
      context.channel().write(Envelope.newBuilder()
          .setRequestId(request.getRequestId())
          .setControl(Control.newBuilder()
              .setError(
                  String.format("Unknown method %s/%s", request.getService(),
                      request.getMethod())))
          .build());
    }
    invoke(method, request.getPayload(), request.getRequestId(), context.channel());
  }

  /**
   * Performs a single method invocation.
   *
   * @param method the method to invoke
   * @param payload the serialized parameter received from the client
   * @param requestId the unique identifier of the request
   * @param channel the channel to use for responding to the client
   * @param <I> the type of the method's parameter
   * @param <O> the return type of the method
   */
  private <I extends Message, O extends Message> void invoke(
      ServerMethod<I, O> method, ByteString payload, long requestId, Channel channel) {
    FutureCallback<O> callback = new ServerMethodCallback<O>(requestId, channel);
    try {
      I request = method.inputParser().parseFrom(payload);
      ListenableFuture<O> result = method.invoke(request);
      pendingRequests.put(requestId, result);
      Futures.addCallback(result, callback);
    } catch (InvalidProtocolBufferException ipbe) {
      callback.onFailure(ipbe);
    }
  }

  /**
   * Encapsulates the logic to execute upon when the invocation of a service
   * method is done.
   *
   * @param <O> the method's return type
   */
  private class ServerMethodCallback<O extends Message> implements FutureCallback<O> {

    private final long requestId;
    private final Channel channel;

    private ServerMethodCallback(long requestId, Channel channel) {
      this.requestId = requestId;
      this.channel = channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSuccess(O result) {
      pendingRequests.remove(requestId);
      Envelope response = Envelope.newBuilder()
          .setPayload(result.toByteString())
          .setRequestId(requestId)
          .build();
      channel.writeAndFlush(response).addListener(new GenericFutureListener<ChannelFuture>() {

        public void operationComplete(ChannelFuture future) {
          if (!future.isSuccess()) {
            logger.warning(String.format(
                "Failed to respond to client on %s ", channel.remoteAddress().toString()));
          }
        }

      });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFailure(Throwable throwable) {
      logger.info("Responding with client failure");
      pendingRequests.remove(requestId);
      Control control = Control.newBuilder()
          .setError(Throwables.getStackTraceAsString(throwable))
          .build();
      Envelope response = Envelope.newBuilder()
          .setControl(control)
          .build();
      channel.writeAndFlush(response).addListener(new GenericFutureListener<ChannelFuture>() {

        public void operationComplete(ChannelFuture future) {
          if (!future.isSuccess()) {
            logger.warning(String.format(
                "Failed to respond to client on %s ", channel.remoteAddress().toString()));
          }
        }

      });
    }

  }
}
