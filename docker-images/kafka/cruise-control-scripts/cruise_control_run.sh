#!/bin/bash
set +x

export CLASSPATH="$CLASSPATH:/opt/cruise-control/libs/*"

# Generate temporary keystore password
export CERTS_STORE_PASSWORD=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c32)

mkdir -p /tmp/cruise-control

# Import certificates into keystore and truststore
$CRUISE_CONTROL_HOME/cruise_control_tls_prepare_certificates.sh

export STRIMZI_TRUSTSTORE_LOCATION=/tmp/cruise-control/replication.truststore.p12
export STRIMZI_TRUSTSTORE_PASSWORD=$CERTS_STORE_PASSWORD

export STRIMZI_KEYSTORE_LOCATION=/tmp/cruise-control/replication.keystore.p12
export STRIMZI_KEYSTORE_PASSWORD=$CERTS_STORE_PASSWORD

# Log directory to use
if [ "x$LOG_DIR" = "x" ]; then
  export LOG_DIR="/tmp"
fi

echo "$KAFKA_LOG4J_OPTS"
if [ -z "$KAFKA_LOG4J_OPTS" ]; then
  export KAFKA_LOG4J_OPTS="-Dlog4j.configuration=file:$CRUISE_CONTROL_HOME/custom-config/log4j.properties"
fi

# enabling Prometheus JMX exporter as Java agent
if [ "$KAFKA_METRICS_ENABLED" = "true" ]; then
  export KAFKA_OPTS="${KAFKA_OPTS} -javaagent:$(ls $KAFKA_HOME/libs/jmx_prometheus_javaagent*.jar)=9404:$CRUISE_CONTROL_HOME/custom-config/metrics-config.yml"
fi

# enabling Tracing agent (initializes Jaeger tracing) as Java agent
if [ "$STRIMZI_TRACING" = "jaeger" ]; then
  export KAFKA_OPTS="$KAFKA_OPTS -javaagent:$(ls $KAFKA_HOME/libs/tracing-agent*.jar)=jaeger"
fi

if [ -z "$KAFKA_HEAP_OPTS" -a -n "${DYNAMIC_HEAP_FRACTION}" ]; then
    . ./dynamic_resources.sh
    # Calculate a max heap size based some DYNAMIC_HEAP_FRACTION of the heap
    # available to a jvm using 100% of the GCroup-aware memory
    # up to some optional DYNAMIC_HEAP_MAX
    CALC_MAX_HEAP=$(get_heap_size ${DYNAMIC_HEAP_FRACTION} ${DYNAMIC_HEAP_MAX})
    if [ -n "$CALC_MAX_HEAP" ]; then
      export KAFKA_HEAP_OPTS="-Xms${CALC_MAX_HEAP} -Xmx${CALC_MAX_HEAP}"
    fi
fi

CRUISE_CONTROL_CONFIG=$CRUISE_CONTROL_HOME/config/cruisecontrol.properties

# TODO Generate default configuration
sed -i "s@bootstrap.servers=.*@bootstrap.servers=$STRIMZI_KAFKA_BOOTSTRAP_SERVERS@g" $CRUISE_CONTROL_CONFIG
sed -i "s@capacity.config.file=.*@capacity.config.file=$CRUISE_CONTROL_HOME/config/capacityJBOD.json@g" $CRUISE_CONTROL_CONFIG
sed -i "s@cluster.configs.file=.*@cluster.configs.file=$CRUISE_CONTROL_HOME/config/clusterConfigs.json@g" $CRUISE_CONTROL_CONFIG

# starting Kafka server with final configuration
exec $CRUISE_CONTROL_HOME/kafka-cruise-control-start.sh $CRUISE_CONTROL_HOME/config/cruisecontrol.properties
