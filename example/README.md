Piezo Example
=============

This directory contains an example of a service surfaced over HTTP (using [Quartz](http://soliton.io/piezo/apidocs/io/soliton/protobuf/quartz/package-summary.html)) and load-balanced through an Nginx reverse-proxy.

The Maven artifact defines a trivial implementation of a time service which can either run in client or in server mode. In server mode, the Quartz server binds to port 10000 intantiates the service and wait forever for requests to serve. In client mode, a service stub is created by connecting to `time.soliton.io` on port 80 and then proceeds to retrieve the time in every timezone known to Joda Time.

I'm running the TimeServer behind Nginx on `time.soliton.io` for testing purposes but this isn't guaranteed to last. The instructions below explain how to run this setup locally.

Why?
----

The reason Quartz was written was to allow surfacing Piezo services on top of a transport which is well-supported and routable by third-party software and hardware, all the while maintaining the compactness and speed of the protocol buffer serialization. Quartz's encoding is proprietary but its use of HTTP allows using proxies, reverse-proxies and load-balancers, the best of which is presently Nginx. You can define the addressing to a pool of servers quite easily.

[TimeService.upstream](https://github.com/jsilland/piezo/blob/master/example/nginx-config/TimeService.upstream) contains the adress of the service instances - basically the definition of your pool. This could be statically defined or, ideally, driven by a group-memebership service such as ZooKeeper.

```conf
upstream TimeService {
  server 127.0.0.1:10000;
  keepalive 16;
}
```

[quartz.conf](https://github.com/jsilland/piezo/blob/master/example/nginx-config/quartz.conf) defines a minimal server that will proxy the incoming requests to the set of upstream service instances.

```conf
include .../TimeService.upstream

location /quartz/soliton.piezo.time.TimeService/ {
  proxy_pass http://TimeService$uri;
  proxy_http_version 1.1;
  proxy_set_header Connection "";
  proxy_read_timeout 60s;
}
```

Configuring Nginx
-----------------

In this example, Nginx is configured as a reverse, load-balancing proxy that sits in between upstream servers (the TimeService instance in this case) and the client. A minimal configuration for the `TimeService` on Quartz is stored in the `nginx-config` directory.

On a Mac, the Nginx installed by `brew` doesn't have the same neat `sites-available` and `sites-enabled` structure you find on Linux, so they need to be created:

```sh
mkdir /usr/local/etc/nginx/sites-enabled
mkdir /usr/local/etc/nginx/sites-available
ln -s /path/to/piezo/source/example/nginx-conf /usr/local/etc/nginx/sites-available/time
ln -s /usr/local/etc/nginx/sites-available/time /usr/local/etc/nginx/sites-enabled/time
```

My main `nginx.conf` root configuration looks like the following:

```conf
worker_processes  1;

events {
    worker_connections  1024;
}

http {
    include mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] '
                    '"$request" $status $bytes_sent '
                    '$upstream_addr $upstream_response_time';

    access_log /var/log/nginx/access.log main;
    error_log /var/log/nginx/error.log debug;

    sendfile on;
    include sites-enabled/**/*.conf;
}
```

Validate the configuration and start Nginx by invoking:

```sh
sudo nginx -t && sudo nginx
```

Running the server
------------------

From the root of the piezo directory, running the following command will:

- Install the bleeding edge snapshot of Piezo
- Compile and run the example time server

```sh
mvn install && mvn -f example/pom.xml compile && mvn -f example/pom.xml exec:exec -Dserver
```

You will need to open a separate shell to run the client.

Running the client
------------------

By default, the client will connect to `time.soliton.io`. You should change the configuration in `example/pom.xml` to specify localhost and then run:

```sh
mvn -f example/pom.xml exec:exec -Dclient
```

If all goes well, you should see something similar to the output below. The client will exit automatically after a little while.

```
[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building Example Time Service and Client 1.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- exec-maven-plugin:1.2.1:exec (default-cli) @ piezo-example ---
Nov 30, 2013 9:45:17 PM io.soliton.protobuf.quartz.QuartzClient$Builder newChannel
INFO: Piezo client successfully connected to localhost:80
Time in Africa/Bujumbura is: December 1, 2013 7:45:18 AM CAT
Time in Africa/Maputo is: December 1, 2013 7:45:18 AM CAT
Time in Africa/Malabo is: December 1, 2013 6:45:18 AM WAT
Time in Africa/Lusaka is: December 1, 2013 7:45:18 AM CAT
Time in Africa/Lubumbashi is: December 1, 2013 7:45:18 AM CAT
Time in Africa/Luanda is: December 1, 2013 6:45:18 AM WAT
Time in Africa/Lome is: December 1, 2013 5:45:18 AM GMT
Time in Africa/Libreville is: December 1, 2013 6:45:18 AM WAT
...
```
