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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
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

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Shared server-side handler implementation for servers whose method calls
 * are encoded using an {@link Envelope} message.
 *
 * @param <I> The incoming request type in which the RPC envelope is encoded
 * @param <O> The outgoing response type in which the RPC envelope will be encoded
 * @author Julien Silland (julien@soliton.io)
 */
public abstract class EnvelopeServerHandler<I, O> extends SimpleChannelInboundHandler<I> {

  public static final Logger logger = Logger.getLogger(
      EnvelopeServerHandler.class.getCanonicalName());

  private final ConcurrentMap<Long, ListenableFuture<?>> pendingRequests = new MapMaker().makeMap();
  private final ExecutorService responseCallbackExecutor = Executors.newCachedThreadPool();

  private final ServiceGroup services;

  public EnvelopeServerHandler(ServiceGroup services) {
    this.services = Preconditions.checkNotNull(services);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSharable() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelRead0(ChannelHandlerContext context, I request)
      throws Exception {
    Envelope envelope = convertRequest(request);
    if (envelope.hasControl() && envelope.getControl().getCancel()) {
      ListenableFuture<?> pending = pendingRequests.remove(envelope.getRequestId());
      if (pending != null) {
        boolean cancelled = pending.cancel(true);
        context.channel().writeAndFlush(Envelope.newBuilder()
            .setRequestId(envelope.getRequestId())
            .setControl(Control.newBuilder().setCancel(cancelled)).build());
      }
      return;
    }

    Service service = services.lookupByName(envelope.getService());
    if (service == null) {
      logger.warning(String.format(
          "Received request for unknown service %s", envelope.getService()));
      context.channel().writeAndFlush(Envelope.newBuilder()
          .setRequestId(envelope.getRequestId())
          .setControl(Control.newBuilder()
              .setError(String.format("Unknown service %s", envelope.getService())))
          .build());
      return;
    }

    ServerMethod<? extends Message, ? extends Message> method =
        service.lookup(envelope.getMethod());
    if (method == null) {
      logger.warning(String.format(
          "Received request for unknown method %s/%s", envelope.getService(),
          envelope.getMethod()));
      context.channel().writeAndFlush(Envelope.newBuilder()
          .setRequestId(envelope.getRequestId())
          .setControl(Control.newBuilder()
              .setError(
                  String.format("Unknown method %s/%s", envelope.getService(),
                      envelope.getMethod())))
          .build());
      return;
    }
    invoke(method, envelope.getPayload(), envelope.getRequestId(), context.channel());
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
      Futures.addCallback(result, callback, responseCallbackExecutor);
    } catch (InvalidProtocolBufferException ipbe) {
      callback.onFailure(ipbe);
    }
  }

  @VisibleForTesting
  public Map<Long, ListenableFuture<?>> pendingRequests() {
    return pendingRequests;
  }

  /**
   * Implemented by subclasses to convert the incoming request into an
   * {@link Envelope}
   *
   * @param request the incoming request
   */
  protected abstract Envelope convertRequest(I request) throws RequestConversionException;

  /**
   * Implemented by subclasses to convert an outgoing response into their
   * specific output type
   *
   * @param response the response to be converted into the output type
   */
  protected abstract O convertResponse(Envelope response);

  /**
   * Encapsulates the logic to execute when the invocation of a service
   * method is done.
   *
   * @param <M> the method's return type
   */
  private class ServerMethodCallback<M extends Message> implements FutureCallback<M> {

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
    public void onSuccess(M result) {
      pendingRequests.remove(requestId);
      Envelope response = Envelope.newBuilder()
          .setPayload(result.toByteString())
          .setRequestId(requestId)
          .build();

      channel.writeAndFlush(convertResponse(response)).addListener(
          new GenericFutureListener<ChannelFuture>() {

            public void operationComplete(ChannelFuture future) {
              if (!future.isSuccess()) {
                logger.warning(String.format(
                    "Failed to respond to client on %s ", channel.remoteAddress().toString()));
              } else {
                logger.info("Successfully responded to client");
              }
            }

          });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFailure(Throwable throwable) {
      logger.info("Responding to client with failure");
      pendingRequests.remove(requestId);
      Control control = Control.newBuilder()
          .setError(Throwables.getStackTraceAsString(throwable))
          .build();
      Envelope response = Envelope.newBuilder()
          .setControl(control)
          .build();
      channel.writeAndFlush(convertResponse(response))
          .addListener(new GenericFutureListener<ChannelFuture>() {

            public void operationComplete(ChannelFuture future) {
              if (!future.isSuccess()) {
                logger.warning(String.format(
                    "Failed to respond to client on %s ", channel.remoteAddress().toString()));
              }
            }

          });
    }

  }

  /**
   * Occurs when an incoming request couldn't be converted into an envelope.
   */
  protected static class RequestConversionException extends Exception {
    private final Object request;

    /**
     * Exhaustive constructor.
     *
     * @param request the request that couldn't be converted
     */
    public RequestConversionException(Object request) {
      this.request = request;
    }

    /**
     * Exhaustive constructor.
     *
     * @param request the request that couldn't be converted
     * @param exception the underlying exception
     */
    public RequestConversionException(Object request, Throwable exception) {
      super(exception);
      this.request = request;
    }

    @Override
    public String getMessage() {
      return String.format("Could not convert incoming request: %s", request);
    }
  }
}
