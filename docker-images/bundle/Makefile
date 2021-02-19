PROJECT_NAME=bundle

CRD_DIR := ../../install/cluster-operator
DEPLOYMENT_FILE := $(CRD_DIR)/060-Deployment-strimzi-cluster-operator.yaml
MANIFESTS := ./manifests
CSV_FILE := $(MANIFESTS)/strimzi-cluster-operator.clusterserviceversion.yaml
DATE := $(shell date +'%Y-%m-%d %H:%M:%S')

CURRENT_BUNDLE_VERSION := $(shell cat ../../release.version)
PREVIOUS_BUNDLE_VERSION := $(shell curl -s https://raw.githubusercontent.com/operator-framework/community-operators/master/community-operators/strimzi-kafka-operator/strimzi-kafka-operator.package.yaml | yq e '.channels[0].currentCSV' -)

update_bundle:
	# Copy Custom Resource Definitions (CRDs)
	$(CP) $(CRD_DIR)/043-Crd-kafkatopic.yaml $(MANIFESTS)/kafkatopics.kafka.strimzi.io.crd.yaml
	$(CP) $(CRD_DIR)/044-Crd-kafkauser.yaml $(MANIFESTS)/kafkausers.kafka.strimzi.io.crd.yaml
	$(CP) $(CRD_DIR)/040-Crd-kafka.yaml $(MANIFESTS)/kafkas.kafka.strimzi.io.crd.yaml
	$(CP) $(CRD_DIR)/041-Crd-kafkaconnect.yaml $(MANIFESTS)/kafkaconnects.kafka.strimzi.io.crd.yaml
	$(CP) $(CRD_DIR)/042-Crd-kafkaconnects2i.yaml $(MANIFESTS)/kafkaconnects2is.kafka.strimzi.io.crd.yaml
	$(CP) $(CRD_DIR)/045-Crd-kafkamirrormaker.yaml $(MANIFESTS)/kafkamirrormakers.kafka.strimzi.io.crd.yaml
	$(CP) $(CRD_DIR)/046-Crd-kafkabridge.yaml $(MANIFESTS)/kafkabridges.kafka.strimzi.io.crd.yaml
	$(CP) $(CRD_DIR)/047-Crd-kafkaconnector.yaml $(MANIFESTS)/kafkaconnectors.kafka.strimzi.io.crd.yaml
	$(CP) $(CRD_DIR)/048-Crd-kafkamirrormaker2.yaml $(MANIFESTS)/kafkamirrormaker2s.kafka.strimzi.io.crd.yaml
	$(CP) $(CRD_DIR)/049-Crd-kafkarebalance.yaml $(MANIFESTS)/kafkarebalances.kafka.strimzi.io.crd.yaml
	$(CP) $(CRD_DIR)/030-ClusterRole-strimzi-kafka-broker.yaml $(MANIFESTS)/strimzikafkabroker.clusterrole.yaml
	$(CP) $(CRD_DIR)/031-ClusterRole-strimzi-entity-operator.yaml $(MANIFESTS)/strimzientityoperator.clusterrole.yaml
	$(CP) $(CRD_DIR)/032-ClusterRole-strimzi-topic-operator.yaml $(MANIFESTS)/strimzitopicoperator.clusterrole.yaml
	$(CP) $(CRD_DIR)/033-ClusterRole-strimzi-kafka-client.yaml $(MANIFESTS)/strimzikafkaclient.clusterrole.yaml
	$(CP) $(CRD_DIR)/050-ConfigMap-strimzi-cluster-operator.yaml $(MANIFESTS)/strimziclusteroperator.configmap.yaml

	# Update exisiting Cluster Service Version (CSV) file
	yq ea -i 'select(fi==0).spec.install.spec.deployments[0].spec = select(fi==1).spec | select(fi==0)' $(CSV_FILE) $(DEPLOYMENT_FILE)
	yq ea -i 'select(fi==0).spec.install.spec.permissions[0].rules = select(fi==1).rules | select(fi==0)' $(CSV_FILE) $(CRD_DIR)/020-ClusterRole-strimzi-cluster-operator-role.yaml
	yq ea -i 'select(fi==0).spec.install.spec.permissions[0].rules = select(fi==1).rules | select(fi==0)' $(CSV_FILE) $(CRD_DIR)/021-ClusterRole-strimzi-cluster-operator-role.yaml

	yq ea -i '.metadata.name = "strimzi-cluster-operator-v$(CURRENT_BUNDLE_VERSION)"' $(CSV_FILE)
	yq ea -i '.spec.install.spec.deployments[0].name = "strimzi-cluster-operator-v$(CURRENT_BUNDLE_VERSION)"' $(CSV_FILE)
	yq ea -i '.spec.version = "$(CURRENT_BUNDLE_VERSION)"' $(CSV_FILE)
	yq ea -i '.spec.replaces = "$(PREVIOUS_BUNDLE_VERSION)" | .spec.replaces style=""' $(CSV_FILE)
	yq ea -i '.metadata.createdAt = "$(DATE)"' $(CSV_FILE)

	# Update relatedImages section  
	$(eval CLUSTER_OPERATOR_IMAGE=$(shell yq eval '.. | select(has("image")) | (select (.name == "*-operator")).image' $(DEPLOYMENT_FILE)))
	$(eval KAFKA_BRIDGE=$(shell yq eval '.. | select(has("name")).env[] | (select (.name == "STRIMZI_DEFAULT_KAFKA_BRIDGE_IMAGE")).value' $(DEPLOYMENT_FILE)))
	$(eval JMXTRANS_IMAGE=$(shell yq eval '.. | select(has("name")).env[] | (select (.name == "STRIMZI_DEFAULT_JMXTRANS_IMAGE")).value' $(DEPLOYMENT_FILE)))
	$(eval KANIKO_EXECUTOR_IMAGE=$(shell yq eval '.. | select(has("name")).env[] | (select (.name == "STRIMZI_DEFAULT_KANIKO_EXECUTOR_IMAGE")).value' $(DEPLOYMENT_FILE)))
	$(eval KAFKA_IMAGE_VALUES=$(shell yq eval '.. | select(has("name")).env[] | (select (.name == "STRIMZI_KAFKA_IMAGES")).value' $(CSV_FILE)))

	yq ea -i '.spec.relatedImages  = [{"name": "strimzi-cluster-operator", "value": "$(CLUSTER_OPERATOR_IMAGE)"}]' $(CSV_FILE)

	# Add Kafka images to relatedImages section. Since make isolates each line into its own subshell,
	# we run following shell commands as one line, joining the lines with ";" and "\"
	for val in $(KAFKA_IMAGE_VALUES); \
	do \
		VERSION=`echo "$${val}" | cut -d'=' -f1 | tr -d '.'`;\
		IMAGE=`echo "$${val}" | cut -d'=' -f2`;\
		NAME="strimzi-kafka-$${VERSION}"\
		name=$${NAME} image="$${IMAGE}" yq ea -i '.spec.relatedImages += [{"name": env(name), "value": env(image)}]' $(CSV_FILE); \
	done\
	
	yq ea -i '.spec.relatedImages += [{"name": "strimzi-bridge", "value": "$(KAFKA_BRIDGE)"}]' $(CSV_FILE)
	yq ea -i '.spec.relatedImages += [{"name": "strimzi-jmxtrans", "value": "$(JMXTRANS_IMAGE)"}]' $(CSV_FILE)
	yq ea -i '.spec.relatedImages += [{"name": "strimzi-kaniko-executor", "value": "$(KANIKO_EXECUTOR_IMAGE)"}]' $(CSV_FILE)

docker_build: update_bundle docker_build_default
	# Update bundle files first

include ../../Makefile.os
include ../../Makefile.docker

.PHONY: build clean release
