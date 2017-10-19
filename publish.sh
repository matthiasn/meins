#!/usr/bin/env bash

cd bundle
yarn install
webpack -p
cd ..

npm update -g electron-builder
npm update -g electron-publisher-s3

rm -rf ./dist
lein dist

PLATFORMS=$1
ELECTRON_BUILDER_COMPRESSION_LEVEL=3

if [ "$2" == "release" ]; then
  echo "Publishing Release"
  electron-builder --publish always $1
else
  echo "Publishing Beta Version"
  electron-builder -c electron-builder-beta.yml --publish always $1
fi
