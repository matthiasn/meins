#!/bin/sh

$JAVA_HOME/bin/javapackager -deploy -native \
                            -outdir packages \
                            -outfile iWasWhere \
                            -srcdir target \
                            -srcfiles iwaswhere-web-0.1.6-standalone.jar \
                            -appclass iwaswhere_web.core \
                            -name "iWasWhere" \
                            -title "iWasWhere"
