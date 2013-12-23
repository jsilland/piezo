Piezo
=====

Piezo is a Java RPC stack that combines [Protocol Buffers](https://code.google.com/p/protobuf/), [Netty](http://netty.io/) and [Guava](https://code.google.com/p/guava-libraries/) in order to provide a state-of-the-art, lightweight, reliable and well-performing channel for deploying and accessing network services.

Piezo integrates with the Protocol Buffer [IDL](https://developers.google.com/protocol-buffers/docs/proto) through a Maven [plugin](https://github.com/jsilland/piezo/tree/master/plugin) which generates Java code from a [service definition](https://developers.google.com/protocol-buffers/docs/proto#services).

Read the [wiki](https://github.com/jsilland/piezo/wiki) or the [Javadoc](http://soliton.io/piezo/apidocs/index.html) for more info.

The build status, hopefully green: [![Build Status](https://travis-ci.org/jsilland/piezo.png)](https://travis-ci.org/jsilland/piezo)

Installation
------------

Piezo 1.0-RC1 is presently available on Maven Central. Please treat this as pre-release software. To bring in Piezo into your build, add the following dependencies to your `pom.xml`:

```xml
<dependencies>
  <dependency>
    <groupId>io.soliton</groupId>
    <artifactId>piezo</artifactId>
    <version>1.0-rc1</version>
  </dependency>
  <dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>15.0</version>
  </dependency>
  <dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>2.5.0</version>
  </dependency>
</dependencies>
```
