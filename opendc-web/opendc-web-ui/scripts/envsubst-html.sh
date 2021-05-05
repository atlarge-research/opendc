#!/bin/sh

set -e

# We can use simple version of envsubst execution as
# envsubst < /usr/share/nginx/html/index.html.template > /usr/share/nginx/html/index.html
# but it replaces everything that looks like environment variable substitution
# so it affects `default values` approach.
# we need to replace only provided environment variables.

auto_envsubst() {
  template_path="/usr/share/nginx/html/index.html.template"
  output_path="/usr/share/nginx/html/index.html"
  defined_envs=$(printf '${%s} ' $(env | cut -d= -f1))
  envsubst "$defined_envs" < "$template_path" > "$output_path"
}

auto_envsubst
exit 0
