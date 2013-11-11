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

import com.google.protobuf.Message;

/**
 * A null-object implementation of the {@link ServerLogger} interface.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class NullServerLogger implements ServerLogger {

  @Override
  public void logMethodCall(Service service, ServerMethod<? extends Message,
      ? extends Message> method) {
  }

  @Override
  public void logServerFailure(ServerMethod<?, ?> serverMethod, Throwable throwable) {
  }

  @Override
  public void logLinkFailure(ServerMethod<?, ?> serverMethod, Throwable cause) {
  }

  @Override
  public void logServerSuccess(ServerMethod<?, ?> serverLogger) {
  }

  @Override
  public void logUnknownService(Service service) {
  }

  @Override
  public void logClientError(Throwable throwable) {
  }

  @Override
  public void logUnknownMethod(Service service, String method) {
  }
}
