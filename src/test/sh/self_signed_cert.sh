#!/bin/sh

openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr
openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt
cat server.key server.crt > server.keycrt
openssl pkcs12 -export -in server.keycrt -out server.p12
base64 -i server.p12 -o server.b64.p12
