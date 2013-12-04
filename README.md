Piezo
=====

Piezo is a Java RPC stack that combines [Protocol Buffers](https://code.google.com/p/protobuf/), [Netty](http://netty.io/) and [Guava](https://code.google.com/p/guava-libraries/) in order to provide a state-of-the-art, lightweight, reliable and well-performing channel for deploying and accessing network services.

Piezo integrates with the Protocol Buffer [IDL](https://developers.google.com/protocol-buffers/docs/proto) through a Maven [plugin](https://github.com/jsilland/piezo/tree/master/plugin) which generates Java code from a [service definition](https://developers.google.com/protocol-buffers/docs/proto#services).

Read the [wiki](https://github.com/jsilland/piezo/wiki) or the [Javadoc](http://soliton.io/piezo/apidocs/index.html) for more info.

The build status, hopefully green: [![Build Status](https://travis-ci.org/jsilland/piezo.png)](https://travis-ci.org/jsilland/piezo)

Installation
------------

Piezo is not yet available in Maven Central, so you'll have to install the framework manually for the time being. This requires a protocol buffer toolchain to be available (see [Usage](https://github.com/jsilland/piezo/wiki/Usage)).

In `piezo`:

    mvn install
