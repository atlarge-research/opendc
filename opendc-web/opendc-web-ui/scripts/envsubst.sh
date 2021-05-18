#!/bin/sh

set -e

auto_envsubst() {
    input_path="/opendc/.next.template"
    output_path="/opendc/.next"

    cp -r "$input_path" "$output_path"
    find "$output_path" -type f -name '*.js' -exec perl -pi -e 's/%%(NEXT_PUBLIC_[_A-Z0-9]+)%%/$ENV{$1}/g' {} \;
}

auto_envsubst
exit 0
