#!/bin/sh
export APVSpwd=1234
java -Xmx1024M  -Dlogback.configurationFile=logback.xml -jar apvs-jetty/target/apvs-jetty.war


