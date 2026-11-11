#!/bin/bash
# usage: healthcheck.sh PORT PATH
# exits 0 only when the HTTP status is 200
exec 3<>/dev/tcp/127.0.0.1/"$1" || exit 1
printf 'GET %s HTTP/1.0\r\nHost: localhost\r\n\r\n' "$2" >&3
head -n1 <&3 | grep -q ' 200'
