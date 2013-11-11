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

import io.soliton.protobuf.AbstractEndToEndTest;
import io.soliton.protobuf.Client;
import io.soliton.protobuf.Server;
import io.soliton.protobuf.TimeServer;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Resources;
import com.google.common.net.HostAndPort;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * End-to-end tests for the encrypted link between a Quartz client and server.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class SslQuartzEndToEndTest extends AbstractEndToEndTest {

  private static QuartzServer server;

  @BeforeClass
  public static void setUp() throws Exception {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    URL url = Resources.getResource(TimeServer.class, "server.b64.p12");
    InputStream keyStoreStream = BaseEncoding.base64().decodingStream(
        Resources.newReaderSupplier(url, Charsets.UTF_8).getInput());
    keyStore.load(keyStoreStream, "password".toCharArray());
    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
    keyManagerFactory.init(keyStore, "password".toCharArray());
    sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
    server = QuartzServer.newServer(findAvailablePort())
        .setSslContext(sslContext)
        .build();
    server.start();
  }

  @AfterClass
  public static void tearDown() {
    server.stop();
  }

  @Override
  protected Server server() {
    return server;
  }

  @Override
  protected Client client() throws Exception {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, TrustfulTrustManagerFactory.getTrustManagers(), null);
    return QuartzClient.newClient(HostAndPort.fromParts("localhost", port))
        .setSslContext(sslContext).build();
  }

  private static final class TrustfulTrustManagerFactory extends TrustManagerFactorySpi {

    private static final Logger logger = Logger.getLogger(
        TrustfulTrustManagerFactory.class.getCanonicalName());

    private static final TrustManager DUMMY_TRUST_MANAGER = new X509TrustManager() {
      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }

      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) {
        logger.warning("Blindly trusting client certificate " + chain[0].getSubjectDN());
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) {
        logger.warning("Blindly trusting server certificate " + chain[0].getSubjectDN());
      }
    };

    public static TrustManager[] getTrustManagers() {
      return new TrustManager[]{DUMMY_TRUST_MANAGER};
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
      return getTrustManagers();
    }

    @Override
    protected void engineInit(KeyStore keystore) throws KeyStoreException {
      // Unused
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
        throws InvalidAlgorithmParameterException {
      // Unused
    }
  }
}
