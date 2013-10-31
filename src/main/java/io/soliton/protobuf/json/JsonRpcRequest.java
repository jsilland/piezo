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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.*;
import com.google.protobuf.Message;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.soliton.protobuf.ServerMethod;
import io.soliton.protobuf.Service;
import io.soliton.protobuf.ServiceGroup;

import java.util.Iterator;

/**
 * Structured representation of a JSON-RPC request.
 *
 * @author Julien Silland (julien@soliton.io)
 */
class JsonRpcRequest {

  private static final Joiner DOT_JOINER = Joiner.on('.');
  private static final Splitter DOT_SPLITTER = Splitter.on('.').trimResults().omitEmptyStrings();


  private final String service;
  private final String method;
  private final JsonElement id;
  private final JsonObject parameter;

  /**
   * Exhaustive constructor
   *
   * @param service the service this call is targeting
   * @param method the method this call is targeting
   * @param id the generic identifier of the request, as set by the client
   * @param parameter the sole parameter of this call
   */
  public JsonRpcRequest(String service, String method, JsonElement id,
      JsonObject parameter) {
    this.service = service;
    this.method = method;
    this.id = id;
    this.parameter = parameter;
  }

  public String service() {
    return service;
  }

  public String method() {
    return method;
  }

  public JsonElement id() {
    return id;
  }

  public JsonObject parameter() {
    return parameter;
  }

  public JsonObject toJson() {
    JsonObject request = new JsonObject();
    request.add(JsonRpcProtocol.ID, id());
    request.add(JsonRpcProtocol.METHOD, new JsonPrimitive(DOT_JOINER.join(service(), method())));
    JsonArray params = new JsonArray();
    params.add(parameter());
    request.add(JsonRpcProtocol.PARAMETERS, params);
    return request;
  }

  public static JsonRpcRequest fromJson(JsonElement root) throws JsonRpcError {
    if (!root.isJsonObject()) {
      throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "Received payload is not a JSON Object");
    }

    JsonObject request = root.getAsJsonObject();
    JsonElement id = request.get(JsonRpcProtocol.ID);
    JsonElement methodNameElement = request.get(JsonRpcProtocol.METHOD);
    JsonElement paramsElement = request.get(JsonRpcProtocol.PARAMETERS);

    if (id == null) {
      throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "Malformed request, missing 'id' property");
    }

    if (methodNameElement == null) {
      throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "Malformed request, missing 'method' property");
    }

    if (paramsElement == null) {
      throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "Malformed request, missing 'params' property");
    }


    if (!methodNameElement.isJsonPrimitive()) {
      throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "Method name is not a JSON primitive");
    }

    JsonPrimitive methodName = methodNameElement.getAsJsonPrimitive();
    if (!methodName.isString()) {
      throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "Method name is not a string");
    }

    if (!paramsElement.isJsonArray()) {
      throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "'params' property is not an array");
    }

    JsonArray params = paramsElement.getAsJsonArray();
    if (params.size() != 1) {
      throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "'params' property is not an array");
    }

    JsonElement paramElement = params.get(0);
    if (!paramElement.isJsonObject()) {
      throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "Parameter is not an object");
    }

    JsonObject parameter = paramElement.getAsJsonObject();
    Iterator<String> serviceAndMethod = DOT_SPLITTER.split(methodName.getAsString()).iterator();

    if (!serviceAndMethod.hasNext()) {
      throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "'method' property is not properly formatted");
    }

    String service = serviceAndMethod.next();
    if (!serviceAndMethod.hasNext()) {
      throw new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "'method' property is not properly formatted");
    }

    String method = serviceAndMethod.next();
    return new JsonRpcRequest(service, method, id, parameter);
  }

  /**
   * Executes this request asynchronously.
   *
   * @param services the context of defined services in which this request is
   *    executing
   * @return a handle on the future result of the invocation
   */
  public ListenableFuture<JsonRpcResponse> invoke(ServiceGroup services) {
    Service service = services.lookupByName(service());
    if (service == null) {
      JsonRpcError error = new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "Unknown service: " + service());
      JsonRpcResponse response = JsonRpcResponse.error(error);
      return Futures.immediateFuture(response);
    }

    ServerMethod<? extends Message, ? extends Message> method = service.lookup(method());

    if (method == null) {
      JsonRpcError error = new JsonRpcError(HttpResponseStatus.BAD_REQUEST,
          "Unknown method: " + service());
      JsonRpcResponse response = JsonRpcResponse.error(error);
      return Futures.immediateFuture(response);
    }

    return invoke(method, parameter(), id());
  }

  /**
   * Actually invokes the server method.
   *
   * @param method the method to invoke
   * @param parameter the request's parameter
   * @param id the request's client-side identifier
   * @param <I> the method's input proto-type
   * @param <O> the method's output proto-type
   */
  private <I extends Message, O extends Message> ListenableFuture<JsonRpcResponse> invoke(
      ServerMethod<I, O> method, JsonObject parameter, JsonElement id) {
    I request;
    try {
      request = (I) Messages.fromJson(method.inputBuilder(), parameter);
    } catch (Exception e) {
      SettableFuture<JsonRpcResponse> future = SettableFuture.create();
      future.setException(e);
      return future;
    }
    ListenableFuture<O> response = method.invoke(request);
    return Futures.transform(response, new JsonConverter());
  }

  private class JsonConverter implements Function<Message, JsonRpcResponse> {

    @Override
    public JsonRpcResponse apply(Message output) {
      return JsonRpcResponse.success(Messages.toJson(output), id());
    }
  }
}
