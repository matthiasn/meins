#!/usr/bin/env bash

#npm version patch

cd bundle
yarn install
webpack -p
cd ..

npm update -g electron-builder
npm update -g electron-publisher-s3

export ELECTRON_BUILDER_COMPRESSION_LEVEL=3
#export DEBUG=electron-builder,electron-builder:*
electron-builder -c electron-builder-beta.yml --publish always -m zip

open dist
