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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Structured representation of a JSON-RPC response.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class JsonRpcResponse {

  private final JsonElement id;
  private final JsonRpcError error;
  private final JsonObject result;

  /**
   * Builds a new response containing only an error.
   *
   * @param error the error to return to the user.
   */
  static JsonRpcResponse error(JsonRpcError error) {
    return error(error, null);
  }

  /**
   * Builds a new response for an identifier request and containing an error.
   *
   * @param error the error to return to the user
   * @param id the identifier of the request for which this response if
   *    generated
   */
  static JsonRpcResponse error(JsonRpcError error, JsonElement id) {
    return new JsonRpcResponse(id, error, null);
  }

  /**
   * Builds a new, successful response.
   *
   * @param payload the object to return to the user.
   * @param id the identifier of the request for which this response is
   *    generated
   */
  public static JsonRpcResponse success(JsonObject payload, JsonElement id) {
    return new JsonRpcResponse(id, null, payload);
  }

  /**
   * Exhaustive constructor.
   *
   * @param id
   * @param error
   * @param result
   */
  private JsonRpcResponse(JsonElement id, JsonRpcError error, JsonObject result) {
    this.id = id;
    this.error = error;
    this.result = result;
  }

  /**
   * Generates the JSON representation of this response.
   */
  public JsonObject toJson() {
    JsonObject body = new JsonObject();
    body.add(JsonRpcProtocol.ID, id());

    if (isError()) {
      body.add(JsonRpcProtocol.ERROR, error().toJson());
    } else {
      body.add(JsonRpcProtocol.RESULT, result());
    }
    return body;
  }

  public static JsonRpcResponse fromJson(JsonObject response) {
    JsonElement id = null;
    JsonRpcError error = null;
    JsonObject result = null;

    if (response.has(JsonRpcProtocol.ID)) {
      id = response.get(JsonRpcProtocol.ID);
    }

    if (response.has(JsonRpcProtocol.ERROR) && response.get(JsonRpcProtocol.ERROR).isJsonObject()) {
      error = JsonRpcError.fromJson(
          response.get(JsonRpcProtocol.ERROR).getAsJsonObject());
    } else if (response.has(JsonRpcProtocol.RESULT) && response.get(JsonRpcProtocol.RESULT)
        .isJsonObject()) {
      result = response.get(JsonRpcProtocol.RESULT).getAsJsonObject();
    }

    if (id == null) {
      error = new JsonRpcError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
          "Missing response identifier");
    }

    if (error == null && result == null) {
      error = new JsonRpcError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Unknown error");
    }

    return new JsonRpcResponse(id, error, result);
  }

  public boolean isError() {
    return error != null;
  }

  public JsonElement id() {
    return id;
  }

  public JsonRpcError error() {
    return error;
  }

  public JsonObject result() {
    return result;
  }
}
