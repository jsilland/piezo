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

import io.soliton.protobuf.Envelope;
import io.soliton.protobuf.EnvelopeClientHandler;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringEncoder;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Client-side handler in charge of decoding and dispatching the responses
 * received form the server.
 *
 * @author Julien Silland (julien@soliton.io)
 */
class QuartzClientHandler extends EnvelopeClientHandler<HttpRequest, HttpResponse> {

  private String path;

  /**
   * {@inheritDoc}
   */
  @Override
  public HttpRequest convertRequest(Envelope request) {
    ByteBuf requestBuffer = Unpooled.buffer(request.getSerializedSize());
    try {
      OutputStream outputStream = new ByteBufOutputStream(requestBuffer);
      request.writeTo(outputStream);
      outputStream.flush();
    } catch (IOException e) {
      // deliberately ignored, as the underlying operation doesn't involve I/O
    }

    String uriPath = String.format("%s%s/%s", path, request.getService(), request.getMethod());
    FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
        HttpMethod.POST, new QueryStringEncoder(uriPath).toString(), requestBuffer);
    httpRequest.headers().set(HttpHeaders.Names.CONTENT_LENGTH, requestBuffer.readableBytes());
    httpRequest.headers().set(HttpHeaders.Names.CONTENT_TYPE, QuartzProtocol.CONTENT_TYPE);
    return httpRequest;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Envelope convertResponse(HttpResponse response) throws ResponseConversionException {
    if (!(response instanceof HttpContent)) {
      throw new ResponseConversionException(response);
    }

    HttpContent content = (HttpContent) response;
    try {
      return Envelope.PARSER.parseFrom(content.content().array());
    } catch (InvalidProtocolBufferException ipbe) {
      throw new ResponseConversionException(response, ipbe);
    }
  }

  public void setPath(String path) {
    this.path = path;
  }
}
