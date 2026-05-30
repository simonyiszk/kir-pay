#!/bin/sh
set -e

HTML=/usr/share/nginx/html/index.html

for var in $(env | grep '^VITE_' | cut -d= -f1); do
  placeholder="__${var}__"
  sed -i "s|${placeholder}|${!var}|g" "$HTML"
done

exec nginx -g "daemon off;"
