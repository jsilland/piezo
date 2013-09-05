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

import com.google.common.base.Throwables;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Handler implementing the decoding and dispatching of RPC calls in
 * an {@link RpcServer}.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<Envelope> {

  public static final Logger logger = Logger.getLogger(RpcServerHandler.class.getCanonicalName());

  private final ConcurrentMap<Long, ListenableFuture<?>> pendingRequests = new MapMaker().makeMap();
  private final ServiceGroup serviceGroup;
  private final ExecutorService responseCallbackExecutor = Executors.newCachedThreadPool();

  RpcServerHandler(ServiceGroup serviceGroup) {
    this.serviceGroup = serviceGroup;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSharable() {
    return true;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext context, Envelope request)
      throws Exception {
    if (request.hasControl() && request.getControl().getCancel()) {
      ListenableFuture<?> pending = pendingRequests.remove(request.getRequestId());
      if (pending != null) {
        boolean cancelled = pending.cancel(true);
        context.channel().write(Envelope.newBuilder()
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
    FutureCallback<O> callback = new ServerMethodCallback<>(requestId, channel);
    try {
      I request = method.inputParser().parseFrom(payload);
      ListenableFuture<O> result = method.invoke(request);
      pendingRequests.put(requestId, result);
      Futures.addCallback(result, callback, responseCallbackExecutor);
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
