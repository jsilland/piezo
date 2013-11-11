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

import io.soliton.protobuf.testing.TestingSingleFile;
import io.soliton.protobuf.testing.TestingSingleFile.DnsResponse;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Trivial DNS service implementation, for testing purposes.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public final class DnsServer implements TestingSingleFile.Dns.Interface {

  @Override
  public ListenableFuture<DnsResponse> resolve(
      TestingSingleFile.DnsRequest request) {
    DnsResponse response = DnsResponse.newBuilder()
        .setIpAddress(1234567).build();
    return Futures.immediateFuture(response);
  }
}
