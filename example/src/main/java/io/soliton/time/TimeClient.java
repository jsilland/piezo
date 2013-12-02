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
package io.soliton.time;

import io.soliton.protobuf.quartz.QuartzClient;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.base.Throwables;
import com.google.common.io.Resources;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Example usage of the Quartz client.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class TimeClient {

  @Parameter(names = "--port", description = "The TCP port the client should connect to")
  private Integer port = 443;

  @Parameter(names = "--host", description = "Hostname the client should connect to")
  private String hostname = "time.soliton.io";

  @Parameter(names = "--ssl", description = "Enables SSL on the client")
  private boolean ssl = false;

  private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

  public static void main(String... args) throws Exception {
    // Parse arguments
    TimeClient timeClient = new TimeClient();
    new JCommander(timeClient, args);

    // Create client
    QuartzClient.Builder clientBuilder = QuartzClient.newClient(
        HostAndPort.fromParts(timeClient.hostname, timeClient.port));

    if (timeClient.ssl) {
      clientBuilder.setSslContext(getSslContext());
    }

    QuartzClient client = clientBuilder.build();

    // Create service stub
    Time.TimeService.Interface timeService = Time.TimeService.newStub(client);

    CountDownLatch latch = new CountDownLatch(DateTimeZone.getAvailableIDs().size());

    // For each known timezone, request its current local time.
    for (String timeZoneId : DateTimeZone.getAvailableIDs()) {
      DateTimeZone timeZone = DateTimeZone.forID(timeZoneId);
      Time.TimeRequest request = Time.TimeRequest.newBuilder().setTimezone(timeZoneId).build();
      Futures.addCallback(timeService.getTime(request), new Callback(latch, timeZone), EXECUTOR);
    }

    latch.await();
    client.close();
  }

  private static SSLContext getSslContext() throws Exception {
    // startcom.crt contains the root and intermediate certificates
    URL certificates = Resources.getResource(TimeClient.class, "startcom.crt");
    InputStream certificatesStream = Resources.newInputStreamSupplier(certificates).getInput();

    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(null, null);
    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    int i = 0;
    for (Certificate cert : certificateFactory.generateCertificates(certificatesStream)) {
      keyStore.setCertificateEntry(String.valueOf(i), cert);
      i++;
    }
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
    trustManagerFactory.init(keyStore);
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
    return sslContext;
  }

  public static final class Callback implements FutureCallback<Time.TimeResponse> {

    private static final DateTimeFormatter FORMAT = DateTimeFormat.longDateTime();

    private final CountDownLatch latch;
    private final DateTimeZone timeZone;

    public Callback(CountDownLatch latch, DateTimeZone timeZone) {
      this.latch = latch;
      this.timeZone = timeZone;
    }

    @Override
    public void onSuccess(Time.TimeResponse response) {
      latch.countDown();
      DateTime responseDateTime = new DateTime(response.getTime(), timeZone);
      System.out.println("Time in " + timeZone.getID() + " is: " + FORMAT.print(responseDateTime));
    }

    @Override
    public void onFailure(Throwable throwable) {
      latch.countDown();
      System.out.println("Failed to obtain time for " + timeZone.getID());
      System.out.println(Throwables.getStackTraceAsString(throwable));
    }
  }
}
