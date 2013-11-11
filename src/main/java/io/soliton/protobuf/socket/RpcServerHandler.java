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

package io.soliton.protobuf.socket;

import io.soliton.protobuf.Envelope;
import io.soliton.protobuf.EnvelopeServerHandler;
import io.soliton.protobuf.ServiceGroup;

/**
 * Handler implementing the decoding and dispatching of RPC calls in
 * an {@link RpcServer}.
 *
 * @author Julien Silland (julien@soliton.io)
 */
class RpcServerHandler extends EnvelopeServerHandler<Envelope, Envelope> {

  RpcServerHandler(ServiceGroup serviceGroup) {
    super(serviceGroup);
  }

  @Override
  protected Envelope convertRequest(Envelope request) throws RequestConversionException {
    return request;
  }

  @Override
  protected Envelope convertResponse(Envelope response) {
    return response;
  }
}
