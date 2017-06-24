#!/usr/bin/env bash

PORT=7778 UPLOAD_PORT=4444 DATA_PATH="${HOME}/iWasWhere_electron/data" java -Djava.awt.headless=true -Dapple.awt.UIElement=true -jar iwaswhere-web-0.1.45-standalone.jar
