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

/**
 * Contains static constants pertaining to the JSON-RPC protocol.
 *
 * @author Julien Silland (julien@soliton.io)
 */
class JsonRpcProtocol {

  public static final String DEFAULT_RPC_PATH = "/rpc";
  public static final String CONTENT_TYPE = "application/json";
  public static final String ID = "id";
  public static final String ERROR = "error";
  public static final String METHOD = "method";
  public static final String PARAMETERS = "params";
  public static final String RESULT = "result";

}
