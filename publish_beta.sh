#!/usr/bin/env bash

#npm version patch

cd bundle
yarn install
webpack -p
cd ..

export ELECTRON_BUILDER_COMPRESSION_LEVEL=3
DEBUG=electron-builder,electron-builder:* electron-builder -c electron-builder-beta.yml --publish always -m zip

open dist
