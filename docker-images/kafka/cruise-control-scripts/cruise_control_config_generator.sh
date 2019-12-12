#!/usr/bin/env bash

CC_CAPACITY_FILE="/tmp/capacity.json"
CC_CLUSTER_CONFIG_FILE="/tmp/clusterConfig.json"

# Generate capacity file
# TODO: Update DISK value based on volume sizes
cat <<EOF > $CC_CAPACITY_FILE
{
	"brokerCapacities": [{
		"brokerId": "-1",
		"capacity": {
			"DISK": "100000",
			"CPU": "100",
			"NW_IN": "10000",
			"NW_OUT": "10000"
		},
		"doc": "This is the default capacity. Capacity unit used for disk is in MB, cpu is in percentage, network throughput is in KB."
	}]
}
EOF

# Generate cluster config
# TODO: Add Kafka configurations here like "min.insync.replicas" for CC reporting
cat <<EOF > $CC_CLUSTER_CONFIG_FILE
{
  "an.example.cluster.config": false
}
EOF

CC_METRIC_SAMPLE_TOPIC_NAME="${CLUSTER_NAME}__KafkaCruiseControlPartitionMetricSamples"
CC_TRAINING_SAMPLE_TOPIC_NAME="${CLUSTER_NAME}__KafkaCruiseControlModelTrainingSamples"

# Write the config file
cat <<EOF
bootstrap.servers=$STRIMZI_KAFKA_BOOTSTRAP_SERVERS
zookeeper.connect=localhost:2181
partition.metric.sample.store.topic=$CC_METRIC_SAMPLE_TOPIC_NAME
broker.metric.sample.store.topic=$CC_TRAINING_SAMPLE_TOPIC_NAME
capacity.config.file=$CC_CAPACITY_FILE
cluster.configs.file=$CC_CLUSTER_CONFIG_FILE
${CRUISE_CONTROL_CONFIGURATION}
EOF
