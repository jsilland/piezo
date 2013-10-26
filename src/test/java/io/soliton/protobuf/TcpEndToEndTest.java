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

import com.google.common.base.Throwables;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.soliton.protobuf.testing.TestingSingleFile;
import io.soliton.protobuf.testing.TimeRequest;
import io.soliton.protobuf.testing.TimeResponse;
import io.soliton.protobuf.testing.TimeService;
import org.joda.time.DateTimeZone;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class TcpEndToEndTest {

  private static RpcServer server;

  private static final class DnsServer implements TestingSingleFile.Dns.Interface {

    @Override
    public ListenableFuture<TestingSingleFile.DnsResponse> resolve(
        TestingSingleFile.DnsRequest request) {
      TestingSingleFile.DnsResponse response = TestingSingleFile.DnsResponse.newBuilder()
          .setIpAddress(1234567).build();
      return Futures.immediateFuture(response);
    }
  }

  @BeforeClass
  public static void setUp() throws Exception {
    server = new RpcServer(10000);
    Service timeService = TimeService.newService(new TimeServer());
    Service dnsService = TestingSingleFile.Dns.newService(new DnsServer());
    server.serviceGroup().addService(timeService);
    server.serviceGroup().addService(dnsService);
    server.start();
  }

  @AfterClass
  public static void tearDown() {
    server.stop();
  }

  @Test
  public void testRequestResponseMultiFile() throws InterruptedException, IOException {

    TimeService.Interface client = TimeService.newStub(
        RpcClient.to(HostAndPort.fromParts("localhost", 10000)));
    TimeRequest request = TimeRequest.newBuilder().setTimezone(DateTimeZone.UTC.getID()).build();

    final CountDownLatch latch = new CountDownLatch(1);
    Futures.addCallback(client.getTime(request), new FutureCallback<TimeResponse>() {
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
    }, Executors.newCachedThreadPool());
    latch.await();
  }

  @Test
  public void testRequestResponseSingleFile() throws InterruptedException, IOException {

    TestingSingleFile.Dns.Interface client = TestingSingleFile.Dns.newStub(
        RpcClient.to(HostAndPort.fromParts("localhost", 10000)));
    TestingSingleFile.DnsRequest request = TestingSingleFile.DnsRequest.newBuilder()
        .setDomain("Castro.local").build();

    final CountDownLatch latch = new CountDownLatch(1);
    Futures.addCallback(client.resolve(request), new FutureCallback<TestingSingleFile.DnsResponse>() {
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
    }, Executors.newCachedThreadPool());
    latch.await();
  }
}
