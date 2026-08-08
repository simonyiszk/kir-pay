#!/bin/sh
set -e

ASSETS=/opt/app-assets
ROOT=/usr/share/nginx/html
HTML=$ROOT/index.html

cp -a "$ASSETS"/. "$ROOT"/

for var in $(env | grep '^VITE_' | cut -d= -f1); do
  placeholder="__${var}__"
  value=$(printenv "$var" | sed 's/[&/\]/\\&/g')
  sed -i "s|${placeholder}|${value}|g" "$HTML"
done

exec nginx -g "daemon off;"
