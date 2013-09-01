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
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;

/**
 * A implementation of {@link ListenableFuture} tailored to handle responses
 * received from RPC servers.
 * 
 * <p>This implementation supports executing custom logic upon a call to the
 * {@link #cancel(boolean)} method.
 *
 * @author Julien Silland (julien@soliton.io)
 */
class ResponseFuture<V> extends AbstractFuture<V> {

  private final Runnable runOnCancel;
  private final Parser<V> parser;

  public ResponseFuture(Runnable runOnCancel, Parser<V> parser) {
    this.runOnCancel = Preconditions.checkNotNull(runOnCancel);
    this.parser = Preconditions.checkNotNull(parser);
  }

  /**
   * Sets the response envelope of this promise.
   *
   * @param response the envelope of the response.
   */
  public void set(Envelope response) {
    try {
      set(parser.parseFrom(response.getPayload()));
    } catch (InvalidProtocolBufferException ipbe) {
      setException(ipbe);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean set(V value) {
    return super.set(value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean setException(Throwable throwable) {
    return super.setException(throwable);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (super.cancel(mayInterruptIfRunning)) {
      runOnCancel.run();
      return true;
    }
    return false;
  }
}
