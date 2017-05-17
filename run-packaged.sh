#!/usr/bin/env bash

PORT=7777 UPLOAD_PORT=3002 DATA_PATH="${HOME}/iWasWhere/data" java -Djava.awt.headless=true -Dapple.awt.UIElement=true -jar iwaswhere-web-0.1.44-standalone.jar
