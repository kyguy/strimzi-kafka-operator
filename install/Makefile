PROJECT_NAME=install

include ../Makefile.os

RELEASE_VERSION ?= latest
RELEASE_PATH ?= ../strimzi-$(RELEASE_VERSION)/$(PROJECT_NAME)

CRD_DIR := ./cluster-operator
DEPLOYMENT_FILE := $(CRD_DIR)/060-Deployment-strimzi-cluster-operator.yaml
MANIFESTS := ./olm-manifests
CSV_FILE := $(MANIFESTS)/bundle.clusterserviceversion.yaml
DATE := $(shell date +'%Y-%m-%d %H:%M:%S')

CURRENT_BUNDLE_VERSION := $(shell cat ../release.version)
PREVIOUS_BUNDLE_VERSION := $(shell curl -s https://raw.githubusercontent.com/operator-framework/community-operators/master/community-operators/strimzi-kafka-operator/strimzi-kafka-operator.package.yaml | yq e '.channels[0].currentCSV' -)

crd_install_helm3:
	$(CP) ./cluster-operator/043-Crd-kafkatopic.yaml ./topic-operator/04-Crd-kafkatopic.yaml
	$(CP) ./cluster-operator/044-Crd-kafkauser.yaml ./user-operator/04-Crd-kafkauser.yaml
	$(CP) ./cluster-operator/040-Crd-kafka.yaml ../helm-charts/helm3/strimzi-kafka-operator/crds/040-Crd-kafka.yaml
	$(CP) ./cluster-operator/041-Crd-kafkaconnect.yaml ../helm-charts/helm3/strimzi-kafka-operator/crds/041-Crd-kafkaconnect.yaml
	$(CP) ./cluster-operator/042-Crd-kafkaconnects2i.yaml ../helm-charts/helm3/strimzi-kafka-operator/crds/042-Crd-kafkaconnects2i.yaml
	$(CP) ./cluster-operator/043-Crd-kafkatopic.yaml ../helm-charts/helm3/strimzi-kafka-operator/crds/043-Crd-kafkatopic.yaml
	$(CP) ./cluster-operator/044-Crd-kafkauser.yaml ../helm-charts/helm3/strimzi-kafka-operator/crds/044-Crd-kafkauser.yaml
	$(CP) ./cluster-operator/045-Crd-kafkamirrormaker.yaml ../helm-charts/helm3/strimzi-kafka-operator/crds/045-Crd-kafkamirrormaker.yaml
	$(CP) ./cluster-operator/046-Crd-kafkabridge.yaml ../helm-charts/helm3/strimzi-kafka-operator/crds/046-Crd-kafkabridge.yaml
	$(CP) ./cluster-operator/047-Crd-kafkaconnector.yaml ../helm-charts/helm3/strimzi-kafka-operator/crds/047-Crd-kafkaconnector.yaml
	$(CP) ./cluster-operator/048-Crd-kafkamirrormaker2.yaml ../helm-charts/helm3/strimzi-kafka-operator/crds/048-Crd-kafkamirrormaker2.yaml
	$(CP) ./cluster-operator/049-Crd-kafkarebalance.yaml ../helm-charts/helm3/strimzi-kafka-operator/crds/049-Crd-kafkarebalance.yaml
	yq eval -i '.metadata.labels.component="kafkas.kafka.strimzi.io-crd"' ../helm-charts/helm3/strimzi-kafka-operator/crds/040-Crd-kafka.yaml
	yq eval -i '.metadata.labels.component="kafkaconnects.kafka.strimzi.io-crd"' ../helm-charts/helm3/strimzi-kafka-operator/crds/041-Crd-kafkaconnect.yaml 
	yq eval -i '.metadata.labels.component="kafkaconnects2is.kafka.strimzi.io-crd"' ../helm-charts/helm3/strimzi-kafka-operator/crds/042-Crd-kafkaconnects2i.yaml 
	yq eval -i '.metadata.labels.component="kafkatopics.kafka.strimzi.io-crd"' ../helm-charts/helm3/strimzi-kafka-operator/crds/043-Crd-kafkatopic.yaml 
	yq eval -i '.metadata.labels.component="kafkausers.kafka.strimzi.io-crd"' ../helm-charts/helm3/strimzi-kafka-operator/crds/044-Crd-kafkauser.yaml 
	yq eval -i '.metadata.labels.component="kafkamirrormakers.kafka.strimzi.io-crd"' ../helm-charts/helm3/strimzi-kafka-operator/crds/045-Crd-kafkamirrormaker.yaml 
	yq eval -i '.metadata.labels.component="kafkabridges.kafka.strimzi.io-crd"' ../helm-charts/helm3/strimzi-kafka-operator/crds/046-Crd-kafkabridge.yaml 
	yq eval -i '.metadata.labels.component="kafkaconnectors.kafka.strimzi.io-crd"' ../helm-charts/helm3/strimzi-kafka-operator/crds/047-Crd-kafkaconnector.yaml 
	yq eval -i '.metadata.labels.component="kafkamirrormaker2.kafka.strimzi.io-crd"' ../helm-charts/helm3/strimzi-kafka-operator/crds/048-Crd-kafkamirrormaker2.yaml 
	yq eval -i '.metadata.labels.component="kafkarebalances.kafka.strimzi.io-crd"' ../helm-charts/helm3/strimzi-kafka-operator/crds/049-Crd-kafkarebalance.yaml 

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
	yq ea -i 'select(fi==0).spec.install.spec.clusterPermissions[0].rules = select(fi==1).rules | select(fi==0)' $(CSV_FILE) $(CRD_DIR)/021-ClusterRole-strimzi-cluster-operator-role.yaml

	yq ea -i '.metadata.name = "strimzi-cluster-operator-v$(CURRENT_BUNDLE_VERSION)"' $(CSV_FILE)
	yq ea -i '.spec.install.spec.deployments[0].name = "strimzi-cluster-operator-v$(CURRENT_BUNDLE_VERSION)"' $(CSV_FILE)
	yq ea -i '.spec.version = "$(CURRENT_BUNDLE_VERSION)"' $(CSV_FILE)
	yq ea -i '.spec.replaces = "$(PREVIOUS_BUNDLE_VERSION)" | .spec.replaces style=""' $(CSV_FILE)
	yq ea -i '.metadata.createdAt = "$(DATE)"' $(CSV_FILE)

	# Update relatedImages section  
	$(eval CLUSTER_OPERATOR_IMAGE=$(shell yq ea '.. | select(has("image")) | (select (.name == "*-operator")).image' $(DEPLOYMENT_FILE)))
	$(eval KAFKA_BRIDGE_IMAGE=$(shell yq ea '.. | select(has("name")).env[] | (select (.name == "STRIMZI_DEFAULT_KAFKA_BRIDGE_IMAGE")).value' $(DEPLOYMENT_FILE)))
	$(eval JMXTRANS_IMAGE=$(shell yq ea '.. | select(has("name")).env[] | (select (.name == "STRIMZI_DEFAULT_JMXTRANS_IMAGE")).value' $(DEPLOYMENT_FILE)))
	$(eval KANIKO_EXECUTOR_IMAGE=$(shell yq ea '.. | select(has("name")).env[] | (select (.name == "STRIMZI_DEFAULT_KANIKO_EXECUTOR_IMAGE")).value' $(DEPLOYMENT_FILE)))
	
	yq ea -i '.spec.relatedImages = null' $(CSV_FILE)
	
	# Adds the images listed in the IMAGES array to the relatedImages section	
	$(eval IMAGES=$(shell echo "$(CLUSTER_OPERATOR_IMAGE) $(KAFKA_BRIDGE_IMAGE) $(JMXTRANS_IMAGE) $(KANIKO_EXECUTOR_IMAGE)"))
	for image in $(IMAGES); \
	do \
		name=strimzi-`echo "$${image}" | cut -d':' -f1 | cut -d'@' -f1 | rev | cut -d'/' -f1 | rev`;\
		name="$${name}" image="$${image}" yq ea -i '.spec.relatedImages += [{"name": env(name), "image": env(image)}]' $(CSV_FILE); \
	done

	# Add Kafka images to relatedImages section. Since make isolates each line into its own subshell,
	# we run following shell commands as one line, joining the lines with ";" and "\"
	$(eval KAFKA_IMAGE_VALUES=$(shell yq eval '.. | select(has("name")).env[] | (select (.name == "STRIMZI_KAFKA_IMAGES")).value' $(CSV_FILE)))
	for val in $(KAFKA_IMAGE_VALUES); \
	do \
		name=strimzi-kafka-`echo "$${val}" | cut -d'=' -f1 | tr -d '.'`;\
		image=`echo "$${val}" | cut -d'=' -f2`;\
		name=$${name} image="$${image}" yq ea -i '.spec.relatedImages += [{"name": env(name), "image": env(image)}]' $(CSV_FILE); \
	done

# THIS PART IS A ROUGH WIP
generate_shas:
	# Iterates through images listed in newly generated relatedImages section
	# and replaces every occurance of that image with its digests if that image 
	# is on the local machine.
	$(eval IMAGES=$(shell yq eval '.spec.relatedImages[].image' "$(CSV_FILE)"))
	for image in $(IMAGES); \
	do \
		if docker inspect "$${image}" &> /dev/null; then \
			digest=`docker image inspect "$${image}" -f '{{join .RepoDigests "|"}}'`;\
		else \
			if test "$${image#*@sha256:}" != "$${image}"; then \
				echo "PRESERVED";\
				digest="$${image}";\
			else \
				echo "Get digest from remote registry for: $${image}"; \
				tag=`echo $${image} | cut -d':' -f2`; \
				image_without_tag=`echo $${image} | cut -d'@' -f1 | cut -d':' -f1`; \
				registry=`echo $${image_without_tag} | cut -d'/' -f1`;\
				namespace=`echo $${image_without_tag} | rev | cut -d'/' -f2 | rev`; \
				name=`echo $${image_without_tag} | rev | cut -d'/' -f1 | rev`; \
				digest=`curl --silent --header "Accept: application/vnd.docker.distribution.manifest.v2+json" https://$${registry}/v2/$${namespace}/$${name}/manifests/$${tag} | sha256sum | head -c 64`; \
				digest="$${image_without_tag}@sha256:$${digest}"; \
			fi;\
		fi;\
		sed -i "s|$${image}|$${digest}|g" $(CSV_FILE);\
	done

crd_install: crd_install_helm3 update_bundle

release:
	mkdir -p $(RELEASE_PATH)
	$(CP) -r ./cluster-operator $(RELEASE_PATH)/
	$(CP) -r ./user-operator $(RELEASE_PATH)/
	$(CP) -r ./topic-operator $(RELEASE_PATH)/
	$(CP) -r ./strimzi-admin $(RELEASE_PATH)/

.PHONY: all build clean docker_build docker_push docker_tag spotbugs
