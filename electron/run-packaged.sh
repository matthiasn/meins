#!/usr/bin/env bash

echo "Starting JVM"
echo $PWD

PORT=7778 UPLOAD_PORT=3233 DATA_PATH="${HOME}/iWasWhere_electron/data" java -Djava.awt.headless=true -Dapple.awt.UIElement=true -jar iwaswhere-web-0.1.46-standalone.jar
