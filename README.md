Piezo
=====

Piezo is a Java RPC stack that combines Protocol Buffers, Netty and Guava in order to provide a state-of-the-art, lightweight and reliable channel for server-to-server communication.

Piezo is made of two components working hand in hand:

- A plugin for the Protocol Buffer compiler that generates concrete implementations of protobuf services
- A framework that implements a generic RPC transport between client and server
