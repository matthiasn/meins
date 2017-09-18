#!/usr/bin/env bash

cd electron-cljs

#npm version patch
lein build

export ELECTRON_BUILDER_COMPRESSION_LEVEL=3

DEBUG=electron-builder,electron-builder:* electron-builder --publish always -mwl

open dist
cd ..
