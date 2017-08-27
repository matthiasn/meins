#!/usr/bin/env bash

cd electron-cljs

lein clean
lein dist
npm install

export ELECTRON_BUILDER_COMPRESSION_LEVEL=3

DEBUG=electron-builder,electron-builder:* electron-builder --publish always -m zip -w nsis -l AppImage

#DEBUG=electron-builder,electron-builder:* electron-builder --publish always -m zip
#DEBUG=electron-builder,electron-builder:* electron-builder --publish always -w nsis
#DEBUG=electron-builder,electron-builder:* electron-builder --publish always -l AppImage

open dist
cd ..
