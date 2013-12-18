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

import io.soliton.protobuf.ClientMethod;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.protobuf.Message;

/**
 * Represents a handle on the result of the invocation of a method over
 * JSON-RPC.
 * <p/>
 * <p>This implementation is in charge of marshalling the response received
 * from the server into the expected protobuf type.</p>
 *
 * @param <V> the type of this promise.
 * @author Julien Silland (julien@soliton.io)
 */
public final class JsonResponseFuture<V extends Message> extends AbstractFuture<V> {

  private final long requestId;
  private final ClientMethod<V> method;

  public JsonResponseFuture(long requestId, ClientMethod<V> method) {
    this.requestId = requestId;
    this.method = method;
  }

  /**
   * Sets the JSON response of this promise.
   *
   * @param response the RPC response
   */
  public void setResponse(JsonRpcResponse response) {
    if (response.isError()) {
      setException(response.error());
      return;
    }

    try {
      set((V) Messages.fromJson(method.outputBuilder(), response.result()));
    } catch (Exception e) {
      setException(e);
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
   * Returns the identifier of the request to which this response responds to.
   */
  public long requestId() {
    return requestId;
  }

  /**
   * Returns the method for which this response holder was created.
   */
  public ClientMethod<V> method() {
    return method;
  }
}
