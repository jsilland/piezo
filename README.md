Piezo
=====

Ever wished you could define, implement, deploy and access network services in Java? Well, that's already possible. Ever wished it took 5 minutes? With Piezo, it's a simple as:

1. Define a service in the protocol buffer IDL syntax.
2. Implement the generated Java interface.
3. Surface the implementations over, e.g. a vanilla TCP socket. Need to make it available to outside users? Piezo ships with a JSON-RPC implementation over HTTP.
4. Use it right away! Getting a client stub takes all of two lines of code.


In More Details
---------------

Piezo is a Java RPC stack that combines [Protocol Buffers](https://code.google.com/p/protobuf/), [Netty](http://netty.io/) and [Guava](https://code.google.com/p/guava-libraries/) in order to provide a state-of-the-art, lightweight, reliable and well-performing channel for deploying and accessing network services.

Piezo is made of two components working hand in hand:

- A plugin for the Protocol Buffer compiler that generates concrete implementations of protobuf services
- A framework that defines a generic RPC abstraction between client and server
- Implementations of clients and servers — binary/socket based or JSON-RPC/HTTP implementations are available 

The Javadoc for the framework is available [here](http://soliton.io/piezo/apidocs/index.html).

Installation
------------

Piezo is not yet available in Maven Central, so you'll have to install both the plugin and the framework manually. Both installations require a protocol buffer toolchain to be available (see Usage section below).

In `piezo/plugin`:

    mvn install

In `piezo`:

    mvn install

Usage
-----

The following assumes you have working setup of the protocol buffer compiler. Piezo requires version 2.5 of the compiler and framework. Configure a Maven toolchain for your protocol buffer compiler. In `~/.m2/toolchains.xml`:

    <toolchains>
      <toolchain>
        <type>protobuf</type>
        <provides>
          <version>2.5.0</version>
        </provides>
        <configuration>
          <protocExecutable>...</protocExecutable>
        </configuration>
      </toolchain>
    </toolchains>

Add the Piezo framework, the protocol buffer frameowrk and Guava as dependencies:

    <dependencies>
      <dependency>
        <groupId>io.soliton</groupId>
        <artifactId>piezo</artifactId>
        <version>1.0-SNAPSHOT</version>
      </dependency>
      <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <version>14.0.1</version>
      </dependency>
      <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>2.5.0</version>
      </dependency>
    </dependencies>

Define a protocol buffer service in a file placed under `src/main/proto`. In the example below, we'll take the example of a DNS service used to resolve domains names into 32-bit IPv4 addresses. Create `src/main/proto/your/package/dns.proto` with the following contents:

    package your.package.dns;

    message DnsRequest {
      optional string domain = 1;
    }
    
    message DnsResponse {
      optional int32 ip_address = 1;
    }
    
    service Dns {
      rpc Resolve (DnsRequest) returns (DnsResponse);
    }

Configure the invocation of the compiler using maven-protoc-plugin:

    <pluginRepositories>
      <pluginRepository>
        <id>protoc-plugin</id>
        <url>http://sergei-ivanov.github.com/maven-protoc-plugin/repo/releases/</url>
      </pluginRepository>
    </pluginRepositories>

    <plugins>
      <plugin>
        <groupId>com.google.protobuf.tools</groupId>
        <artifactId>maven-protoc-plugin</artifactId>
        <version>0.3.1</version>
        <extensions>true</extensions>
        <executions>
          <execution>
            <id>Generate proto sources</id>
            <goals>
              <goal>compile</goal>
              <goal>testCompile</goal>
            </goals>
            <phase>generate-sources</phase>
            <configuration>
              <protocPlugins>
                <protocPlugin>
                  <id>piezo-plugin</id>
                  <groupId>io.soliton</groupId>
                  <artifactId>piezo-plugin</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <mainClass>io.soliton.protobuf.plugin.PiezoPlugin</mainClass>
                </protocPlugin>
              </protocPlugins>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>

You should now be able to invoke `mvn compile` and check the generated code under `target/generated-sources`. The next step is to create a concrete implementation of the `Dns` service. The Piezo plugin will have generated an abstract class named `Dns` with an inner interface named `Interface`. The concrete implementation will reside on the server. The interface is asynchronous in nature and uses the concurrency facilities provided by Guava:

    public class DnsService implements Dns.Interface {
      public ListenableFuture<DnsResponse> resolve(DnsRequest request) {
        return Futures.immediateFuture(DnsResponse.newBuilder().setIpAddress(12435).build());
      }
    }

Instantiate a server and configure it to surface the DNS service:

    RpcServer server = new RcpServer(10000); // the server will bind to this port
    server.serverGroup().addService(Dns.newService(new DnsService()));
    server.start();

On the client side, instantiate the stub by specifying to which host and port to connect. Then start using the interface as you normally would:

    Client transport = new RpcClient(HostAndPort.fromParts("localhost", 10000));
    Dns.Interface client = Dns.newStub(transport);
    client.resolve("www.google.com").getIpAddress(); // 12345

