#!/usr/bin/env bash

cd electron-cljs

npm version patch
lein dist

export ELECTRON_BUILDER_COMPRESSION_LEVEL=3

#DEBUG=electron-builder,electron-builder:* electron-builder --publish always -m
#DEBUG=electron-builder,electron-builder:* electron-builder --publish always -mwl
DEBUG=electron-builder,electron-builder:* electron-builder --publish always -m zip -w

open dist
cd ..
