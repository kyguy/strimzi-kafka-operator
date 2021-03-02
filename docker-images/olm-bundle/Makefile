PROJECT_NAME=bundle

# TODO: Update versions and channels in bundle.Dockerfile and metadata/annotations.yaml files

update_bundle:
	# TODO: COPY files from ../../install/olm-manifests

docker_build: update_bundle docker_build_default
	# Update bundle files first

include ../../Makefile.os
include ../../Makefile.docker

.PHONY: build clean release
