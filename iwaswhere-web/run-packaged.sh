#!/usr/bin/env bash

PORT=7777 UPLOAD_PORT=3002 DATA_PATH="${HOME}/iWasWhere/data" zulu8.17.0.3-jdk8.0.102-macosx_x64/bin/java -Djava.awt.headless=true -Dapple.awt.UIElement=true -jar iwaswhere-web-0.1.25-standalone.jar
