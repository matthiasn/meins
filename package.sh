#!/usr/bin/env bash

cd electron
electron-packager --overwrite --out out . iWasWhere
open out
cd ..
