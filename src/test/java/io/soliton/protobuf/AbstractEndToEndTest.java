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
import io.soliton.protobuf.testing.TimeRequest;
import io.soliton.protobuf.testing.TimeResponse;
import io.soliton.protobuf.testing.TimeService;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base test class for asserting the behavior of RPC client-server pairs.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public abstract class AbstractEndToEndTest {

  protected static int port;

  protected abstract Server server();

  protected abstract Client client() throws Exception;

  /**
   * Utility method to get an available TCP port.
   *
   * @throws IOException
   */
  protected static int findAvailablePort() throws IOException {
    ServerSocket socket = new ServerSocket(0);
    port = socket.getLocalPort();
    socket.close();
    return port;
  }

  @Test
  public void testRequestResponseMultiFile() throws Exception {
    Service timeService = TimeService.newService(new TimeServer());
    server().serviceGroup().addService(timeService);

    TimeService.Interface timeClient = TimeService.newStub(client());
    TimeRequest request = TimeRequest.newBuilder().setTimezone(DateTimeZone.UTC.getID()).build();

    ExecutorService callbackExecutor = Executors.newCachedThreadPool();
    final CountDownLatch latch = new CountDownLatch(100);
    for (int i = 0; i < 100; i++) {
      Futures.addCallback(timeClient.getTime(request), new FutureCallback<TimeResponse>() {
        @Override
        public void onSuccess(TimeResponse result) {
          Assert.assertTrue(result.getTime() > 0);
          latch.countDown();
        }

        @Override
        public void onFailure(Throwable throwable) {
          Throwables.propagate(throwable);
          latch.countDown();
        }
      }, callbackExecutor);
    }
    Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
  }

  @Test
  public void testRequestResponseSingleFile() throws Exception {
    Service dnsService = TestingSingleFile.Dns.newService(new DnsServer());
    server().serviceGroup().addService(dnsService);

    TestingSingleFile.Dns.Interface client = TestingSingleFile.Dns.newStub(client());
    TestingSingleFile.DnsRequest request = TestingSingleFile.DnsRequest.newBuilder()
        .setDomain("Castro.local").build();

    ExecutorService callbackExecutor = Executors.newCachedThreadPool();
    final CountDownLatch latch = new CountDownLatch(100);
    for (int i = 0; i < 100; i++) {
      Futures.addCallback(client.resolve(request),
          new FutureCallback<TestingSingleFile.DnsResponse>() {
            @Override
            public void onSuccess(TestingSingleFile.DnsResponse result) {
              Assert.assertEquals(1234567, result.getIpAddress());
              latch.countDown();
            }

            @Override
            public void onFailure(Throwable throwable) {
              Throwables.propagate(throwable);
              latch.countDown();
            }
          }, callbackExecutor);
    }
    Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
  }
}
