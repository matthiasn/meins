#!/usr/bin/env bash

cd electron-cljs

npm version patch
lein dist

export ELECTRON_BUILDER_COMPRESSION_LEVEL=3

DEBUG=electron-builder,electron-builder:* electron-builder -c electron-builder-beta.yml --publish always -m zip -w

open dist
cd ..
