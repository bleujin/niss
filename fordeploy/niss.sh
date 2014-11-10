#!/bin/bash
 
 
source "`dirname "$0"`/functions.sh"

add_classpath ${HOMEDIR}
 
add_jvm_args $JVM_PARAMS
add_jvm_args '-Djava.net.preferIPv4Stack=true'
 
# RHQ monitoring options
add_jvm_args '-Dcom.sun.management.jmxremote.ssl=false'
add_jvm_args '-Dcom.sun.management.jmxremote.authenticate=false'
jmxport=$(find_tcp_port)
add_jvm_args -Dcom.sun.management.jmxremote.port=$jmxport
 
# Workaround for JDK6 NPE: http://bugs.sun.com/view_bug.do?bug_ID=6427854
add_jvm_args '-Dsun.nio.ch.bugLevel=""'

#memroy
add_jvm_args '-Xms512m'
add_jvm_args '-Xmx1024m'
 
 
# Sample JPDA settings for remote socket debugging
# add_jvm_args "-Xrunjdwp:transport=dt_socket,address=8686,server=y,suspend=n"
 
# Sample Log4j configuration
# LOG4J_CONFIG=file:///${HOMEDIR}/resource/log4j.properties
 
add_program_args "$@"
add_program_args -config:resource/config/niss-config.xml
# add_program_args -acton:restart

 
echo "jmxport=$jmxport"
# start net.ion.radon.ServerRunner
java -jar niss_0.6.jar