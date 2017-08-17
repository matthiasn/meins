#!/usr/bin/env bash

cd electron
DEBUG=electron-builder,electron-builder:* electron-builder --publish always -m zip
open dist
cd ..
