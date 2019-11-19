#!/bin/bash
# Copyright 2017 LinkedIn Corp. Licensed under the BSD 2-Clause License (the "License").
# See License in the project root for license information.

set +x

if [ $# -lt 1 ];
then
  echo "USAGE: $0 [-daemon] [-name servicename] [-loggc] [-jars jar_path] config_path [port]"
  exit 1
fi

CLASSPATH="$CLASSPATH:/opt/kafka/cc-libs/*"

# JMX settings
if [ -z "$KAFKA_JMX_OPTS" ]; then
  KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false  -Dcom.sun.management.jmxremote.ssl=false "
fi

# JMX port to use
if [  $JMX_PORT ]; then
  KAFKA_JMX_OPTS="$KAFKA_JMX_OPTS -Dcom.sun.management.jmxremote.port=$JMX_PORT "
fi

# Log directory to use
if [ "x$LOG_DIR" = "x" ]; then
  LOG_DIR="$KAFKA_HOME/logs"
fi

# Log4j settings
if [ -z "$KAFKA_LOG4J_OPTS" ]; then
  # Log to console. This is a tool.
  LOG4J_DIR="/opt/kafka/config/log4j.properties"
  KAFKA_LOG4J_OPTS="-Dlog4j.configuration=file:${LOG4J_DIR}"
else
  # create logs directory
  if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
  fi
fi

KAFKA_LOG4J_OPTS="-Dkafka.logs.dir=$LOG_DIR $KAFKA_LOG4J_OPTS"

# Generic jvm settings you want to add
if [ -z "$KAFKA_OPTS" ]; then
  KAFKA_OPTS=""
fi

# Set Debug options if enabled
if [ "x$KAFKA_DEBUG" != "x" ]; then

    # Use default ports
    DEFAULT_JAVA_DEBUG_PORT="5005"

    if [ -z "$JAVA_DEBUG_PORT" ]; then
        JAVA_DEBUG_PORT="$DEFAULT_JAVA_DEBUG_PORT"
    fi

    # Use the defaults if JAVA_DEBUG_OPTS was not set
    DEFAULT_JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=${DEBUG_SUSPEND_FLAG:-n},address=$JAVA_DEBUG_PORT"
    if [ -z "$JAVA_DEBUG_OPTS" ]; then
        JAVA_DEBUG_OPTS="$DEFAULT_JAVA_DEBUG_OPTS"
    fi

    echo "Enabling Java debug options: $JAVA_DEBUG_OPTS"
    KAFKA_OPTS="$JAVA_DEBUG_OPTS $KAFKA_OPTS"
fi

# Which java to use
if [ -z "$JAVA_HOME" ]; then
  JAVA="java"
else
  JAVA="$JAVA_HOME/bin/java"
fi

# Memory options
#if [ -z "$KAFKA_HEAP_OPTS" ]; then
#  KAFKA_HEAP_OPTS="-Xmx1G"
#fi

# JVM performance options
#if [ -z "$KAFKA_JVM_PERFORMANCE_OPTS" ]; then
#  KAFKA_JVM_PERFORMANCE_OPTS="-server -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC -Djava.awt.headless=true"
#fi

#Add jaas file to KAFKA_OPTS if present
#if [ -f $base_dir/config/cruise_control_jaas.conf ]
#then
#  KAFKA_OPTS="-Djava.security.auth.login.config=$base_dir/config/cruise_control_jaas.conf $KAFKA_OPTS"
#fi

DAEMON_NAME="kafka-cruise-control"
CONSOLE_OUTPUT_FILE="${LOG_DIR}"/"${DAEMON_NAME}".out
while [ $# -gt 0 ]; do
  COMMAND=$1
  case $COMMAND in
    -name)
      DAEMON_NAME=$2
      CONSOLE_OUTPUT_FILE="${LOG_DIR}"/"${DAEMON_NAME}".out
      shift 2
      ;;
    -loggc)
      if [ -z "$KAFKA_GC_LOG_OPTS" ]; then
        GC_LOG_ENABLED="true"
      fi
      shift
      ;;
    -daemon)
      DAEMON_MODE="true"
      shift
      ;;
    -jars)
      JAR_PATHS=$2
      copyJars "$JAR_PATHS"
      shift 2
      ;;
    *)
      break
      ;;
  esac
done

# GC options
#GC_FILE_SUFFIX='-gc.log'
#GC_LOG_FILE_NAME=''
#if [ "x$GC_LOG_ENABLED" = "xtrue" ]; then
#  GC_LOG_FILE_NAME=$DAEMON_NAME$GC_FILE_SUFFIX
#  KAFKA_GC_LOG_OPTS="-Xloggc:$LOG_DIR/$GC_LOG_FILE_NAME -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M"
#fi

# Generate temporary keystore password
export CERTS_STORE_PASSWORD=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c32)

mkdir -p /tmp/cruise-control

# Import certificates into keystore and truststore
./cruise_control_tls_prepare_certificates.sh

export STRIMZI_TRUSTSTORE_LOCATION=/tmp/cruise-control/replication.truststore.p12
export STRIMZI_TRUSTSTORE_PASSWORD=$CERTS_STORE_PASSWORD

export STRIMZI_KEYSTORE_LOCATION=/tmp/cruise-control/replication.keystore.p12
export STRIMZI_KEYSTORE_PASSWORD=$CERTS_STORE_PASSWORD

# Launch mode
if [ "x$DAEMON_MODE" = "xtrue" ]; then
  nohup $JAVA $KAFKA_HEAP_OPTS $KAFKA_JVM_PERFORMANCE_OPTS $KAFKA_GC_LOG_OPTS $KAFKA_JMX_OPTS $KAFKA_LOG4J_OPTS -cp $CLASSPATH $KAFKA_OPTS com.linkedin.kafka.cruisecontrol.KafkaCruiseControlMain "$@" > "$CONSOLE_OUTPUT_FILE" 2>&1 < /dev/null &
else
  exec $JAVA $KAFKA_HEAP_OPTS $KAFKA_JVM_PERFORMANCE_OPTS $KAFKA_GC_LOG_OPTS $KAFKA_JMX_OPTS $KAFKA_LOG4J_OPTS -cp $CLASSPATH $KAFKA_OPTS com.linkedin.kafka.cruisecontrol.KafkaCruiseControlMain "$@"
fi
