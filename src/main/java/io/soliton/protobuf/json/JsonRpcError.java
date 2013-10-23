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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Structured representation of a JSON-RPC error.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class JsonRpcError extends Exception {

  private final HttpResponseStatus status;

  /**
   * Exhaustive constructor.
   *
   * @param status the HTTP status to associate with this error
   * @param message a message detailing the cause of this error
   */
  public JsonRpcError(HttpResponseStatus status, String message) {
    super(message);
    this.status = status;
  }

  public JsonObject toJson() {
    JsonObject error = new JsonObject();
    error.add("code", new JsonPrimitive(status.code()));
    error.addProperty("message", getMessage());
    return error;
  }

  public static JsonRpcError fromJson(JsonObject error) {
    int status = error.get("code").getAsInt();
    String message = error.get("message").getAsString();
    return new JsonRpcError(HttpResponseStatus.valueOf(status), message);
  }

  /**
   * Returns the status of this error.
   */
  public HttpResponseStatus status() {
    return status;
  }
}
