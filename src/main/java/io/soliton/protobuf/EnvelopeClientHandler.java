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
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for client-side RPC handlers that encode RPC calls in an
 * {@link Envelope}.
 *
 * @param <I> the type of the outgoing message in which the envelope is wrapped
 * @param <O> the type of the incoming message in which the envelope is wrapped
 * @author Julien Silland (julien@soliton.io)
 */
public abstract class EnvelopeClientHandler<I, O> extends SimpleChannelInboundHandler<O> {

  private static final Logger logger = Logger.getLogger(
      EnvelopeClientHandler.class.getCanonicalName());

  private final ConcurrentMap<Long, EnvelopeFuture<? extends Message>> inFlightRequests =
      new MapMaker().makeMap();
  private Channel channel;
  private ClientLogger clientLogger;

  /**
   * {@inheritDoc}
   */
  @Override
  public void channelRead0(ChannelHandlerContext context, O response) throws Exception {
    Envelope envelope = null;
    try {
      envelope = convertResponse(response);
    } catch (ResponseConversionException rce) {
      logger.log(Level.WARNING, "Failed to convert response", rce);
      return;
    }
    EnvelopeFuture<? extends Message> future = inFlightRequests.remove(envelope.getRequestId());
    if (future == null) {
      logger.warning(String.format("Received response from %s for unknown request id: %d",
          channel.remoteAddress(), envelope.getRequestId()));
      return;
    }
    future.setResponse(envelope);
  }

  /**
   * Converts an outgoing RPC method call into the request type supported by
   * this handler.
   *
   * @param request the outgoing request
   * @return the converted request
   */
  public abstract I convertRequest(Envelope request);

  /**
   * Converts an incoming RPC response from the type supported by this handler
   * into an {@link Envelope}.
   *
   * @param response the response received from the server
   * @return the incoming response
   * @throws ResponseConversionException in case an error happens during the
   * conversion
   */
  public abstract Envelope convertResponse(O response) throws ResponseConversionException;

  /**
   * Returns a new provisional handle on the future result of an RPC
   * invocation.
   * <p/>
   * <p>This handler will keep track of the returned object and set its
   * response when it is received from the server.</p>
   *
   * @param clientMethod the method that is intended to be invoked.
   */
  public <O extends Message> EnvelopeFuture<O> newProvisionalResponse(
      ClientMethod<O> clientMethod) {
    long requestId = ThreadLocalRandom.current().nextLong();
    EnvelopeFuture<O> outputFuture = new EnvelopeFuture<>(requestId, clientMethod,
        new Cancel(requestId), clientLogger);
    inFlightRequests.put(requestId, outputFuture);
    return outputFuture;
  }

  /**
   * Terminates the processing an RPC based on its identifier.
   *
   * @param requestId the identifier of the request to terminate.
   */
  public ListenableFuture<? extends Message> finish(long requestId) {
    return inFlightRequests.remove(requestId);
  }

  /**
   * Sets the channel this handler will use to communicate with the remote
   * server.
   *
   * @param channel a channel connected to the server
   */
  public void setChannel(Channel channel) {
    this.channel = Preconditions.checkNotNull(channel);
  }

  /**
   * Sets the logger this handler should report events to.
   *
   * @param clientLogger a client logger.
   */
  public void setClientLogger(ClientLogger clientLogger) {
    this.clientLogger = Preconditions.checkNotNull(clientLogger);
  }

  /**
   * Returns the mapping in which the RPC requests pending with the server
   * are kept.
   */
  @VisibleForTesting
  public Map<Long, EnvelopeFuture<? extends Message>> inFlightRequests() {
    return inFlightRequests;
  }

  /**
   * Returns the outbound channel connected to the remote server.
   */
  protected Channel channel() {
    return channel;
  }

  /**
   * Implements the cancellation logic for an individual RPC.
   *
   * @author Julien Silland (julien@soliton.io)
   */
  class Cancel implements Runnable {
    private final long requestId;

    public Cancel(long requestId) {
      this.requestId = requestId;
    }

    @Override
    public void run() {
      if (inFlightRequests.remove(requestId) != null) {
        Envelope request = Envelope.newBuilder()
            .setRequestId(requestId)
            .setControl(Control.newBuilder().setCancel(true))
            .build();
        channel.writeAndFlush(convertRequest(request));
      }
    }
  }

  /**
   * Occurs when a received response couldn't be converted into an
   * {@link Envelope}.
   */
  protected static class ResponseConversionException extends Exception {
    private final Object response;

    /**
     * Exhaustive constructor
     *
     * @param response the response that couldn't be converted
     */
    public ResponseConversionException(Object response) {
      this.response = response;
    }

    /**
     * Exhaustive constructor
     *
     * @param response the response that couldn't be converted
     * @param exception the underlying cause of the failure
     */
    public ResponseConversionException(Object response, Throwable exception) {
      super(exception);
      this.response = response;
    }

    @Override
    public String getMessage() {
      return String.format("Could not convert incoming response: %s", response);
    }
  }
}
