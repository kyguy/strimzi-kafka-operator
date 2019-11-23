#!/bin/bash
set +x

export CLASSPATH="$CLASSPATH:/opt/cruise-control/libs/*"

# Generate temporary keystore password
export CERTS_STORE_PASSWORD=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c32)

mkdir -p /tmp/cruise-control

# Import certificates into keystore and truststore
./$CRUISE_CONTROL_HOME/cruise_control_tls_prepare_certificates.sh

export STRIMZI_TRUSTSTORE_LOCATION=/tmp/cruise-control/replication.truststore.p12
export STRIMZI_TRUSTSTORE_PASSWORD=$CERTS_STORE_PASSWORD

export STRIMZI_KEYSTORE_LOCATION=/tmp/cruise-control/replication.keystore.p12
export STRIMZI_KEYSTORE_PASSWORD=$CERTS_STORE_PASSWORD

# Log directory to use
if [ "x$LOG_DIR" = "x" ]; then
  export LOG_DIR="/tmp"
fi

CRUISE_CONTROL_CONFIG=$CRUISE_CONTROL_HOME/config/cruisecontrol.properties

# TODO Make kafka and zoo address generic
sed -i 's@bootstrap.servers=.*@bootstrap.servers=my-cluster-kafka-bootstrap:9092@g' $CRUISE_CONTROL_CONFIG
sed -i "s@capacity.config.file=.*@capacity.config.file=$CRUISE_CONTROL_HOME/config/capacityJBOD.json@g" $CRUISE_CONTROL_CONFIG
sed -i "s@cluster.configs.file=.*@cluster.configs.file=$CRUISE_CONTROL_HOME/config/clusterConfigs.json@g" $CRUISE_CONTROL_CONFIG

# starting Kafka server with final configuration
exec $CRUISE_CONTROL_HOME/kafka-cruise-control-start.sh $CRUISE_CONTROL_HOME/config/cruisecontrol.properties