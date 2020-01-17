/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.model;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.strimzi.api.kafka.model.ContainerEnvVar;
import io.strimzi.api.kafka.model.CruiseControlResources;
import io.strimzi.api.kafka.model.CruiseControlSpec;
import io.strimzi.api.kafka.model.CruiseControlSpecBuilder;
import io.strimzi.api.kafka.model.InlineLogging;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import io.strimzi.api.kafka.model.storage.EphemeralStorage;
import io.strimzi.api.kafka.model.storage.SingleVolumeStorage;
import io.strimzi.api.kafka.model.storage.Storage;
import io.strimzi.operator.cluster.KafkaVersionTestUtils;
import io.strimzi.operator.cluster.ResourceUtils;
import io.strimzi.operator.common.model.Labels;
import io.strimzi.test.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.strimzi.operator.cluster.model.CruiseControlConfiguration.CC_DEFAULT_PROPERTIES_MAP;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class CruiseControlTest {
    private final String namespace = "test";
    private final String cluster = "foo";
    private final int replicas = 1;
    private final String image = "my-image:latest";
    private final int healthDelay = 120;
    private final int healthTimeout = 30;
    private final String minInsyncReplicas = "2";
    private final Map<String, Object> metricsCm = singletonMap("animal", "wombat");
    private final Map<String, Object> kafkaConfig = singletonMap(CruiseControl.MIN_INSYNC_REPLICAS, minInsyncReplicas);
    private final Map<String, Object> zooConfig = singletonMap("foo", "bar");
    private final Map<String, Object> ccConfig = new HashMap<String, Object>() {{
            putAll(CC_DEFAULT_PROPERTIES_MAP);
            put("num.partition.metrics.windows", "2");
        }};
    private final Storage kafkaStorage = new EphemeralStorage();
    private final SingleVolumeStorage zkStorage = new EphemeralStorage();
    private final InlineLogging kafkaLogJson = new InlineLogging();
    private final InlineLogging zooLogJson = new InlineLogging();
    private final String version = KafkaVersionTestUtils.DEFAULT_KAFKA_VERSION;
    private static final KafkaVersion.Lookup VERSIONS = KafkaVersionTestUtils.getKafkaVersionLookup();
    private final String kafkaHeapOpts = "-Xms" + AbstractModel.DEFAULT_JVM_XMS;
    private final String ccImage = "my-cruise-control-image";

    {
        kafkaLogJson.setLoggers(singletonMap("kafka.root.logger.level", "OFF"));
        zooLogJson.setLoggers(singletonMap("zookeeper.root.logger", "OFF"));
    }

    private final CruiseControlSpec cruiseControlOperator = new CruiseControlSpecBuilder()
            .withImage(ccImage)
            .withReplicas(replicas)
            .withConfig(ccConfig)
            .build();

    private final Kafka resource =
            new KafkaBuilder(ResourceUtils.createKafkaCluster(namespace, cluster, replicas, image, healthDelay, healthTimeout))
            .editSpec()
                .editKafka()
                    .withVersion(version)
                    .withConfig(kafkaConfig)
                .endKafka()
                .withCruiseControl(cruiseControlOperator)
            .endSpec()
            .build();

    private final CruiseControl cc = CruiseControl.fromCrd(resource, VERSIONS);

    public void checkOwnerReference(OwnerReference ownerRef, HasMetadata resource)  {
        assertThat(resource.getMetadata().getOwnerReferences().size(), is(1));
        assertThat(resource.getMetadata().getOwnerReferences().get(0), is(ownerRef));
    }

    private Map<String, String> expectedLabels(String name)    {
        return TestUtils.map(Labels.STRIMZI_CLUSTER_LABEL, this.cluster,
                "my-user-label", "cromulent",
                Labels.STRIMZI_KIND_LABEL, Kafka.RESOURCE_KIND,
                Labels.STRIMZI_NAME_LABEL, name,
                Labels.KUBERNETES_NAME_LABEL, Labels.KUBERNETES_NAME,
                Labels.KUBERNETES_INSTANCE_LABEL, this.cluster,
                Labels.KUBERNETES_MANAGED_BY_LABEL, AbstractModel.STRIMZI_CLUSTER_OPERATOR_NAME);
    }

    private Map<String, String> expectedSelectorLabels()    {
        return Labels.fromMap(expectedLabels()).strimziSelectorLabels().toMap();
    }

    private Map<String, String> expectedLabels()    {
        return expectedLabels(CruiseControlResources.deploymentName(cluster));
    }

    private List<EnvVar> getExpectedEnvVars() {
        List<EnvVar> expected = new ArrayList<>();
        expected.add(new EnvVarBuilder().withName(CruiseControl.ENV_VAR_STRIMZI_KAFKA_BOOTSTRAP_SERVERS).withValue(CruiseControl.defaultBootstrapServers(cluster)).build());
        expected.add(new EnvVarBuilder().withName(KafkaMirrorMakerCluster.ENV_VAR_STRIMZI_KAFKA_GC_LOG_ENABLED).withValue(Boolean.toString(AbstractModel.DEFAULT_JVM_GC_LOGGING_ENABLED)).build());
        expected.add(new EnvVarBuilder().withName(CruiseControl.ENV_VAR_MIN_INSYNC_REPLICAS).withValue(minInsyncReplicas).build());
        expected.add(new EnvVarBuilder().withName(KafkaMirrorMakerCluster.ENV_VAR_KAFKA_HEAP_OPTS).withValue(kafkaHeapOpts).build());
        expected.add(new EnvVarBuilder().withName(CruiseControl.ENV_VAR_CRUISE_CONTROL_CONFIGURATION).withValue(new CruiseControlConfiguration(ccConfig.entrySet()).getConfiguration()).build());

        return expected;
    }

    @Test
    public void testFromConfigMap() {
        assertThat(cc.namespace, is(namespace));
        assertThat(cc.cluster, is(cluster));
        assertThat(cc.getImage(), is(ccImage));
    }

    @Test
    public void testGenerateDeployment() {
        Deployment dep = cc.generateDeployment(null, true, null, null);

        List<Container> containers = dep.getSpec().getTemplate().getSpec().getContainers();

        assertThat(containers.size(), is(2));

        assertThat(dep.getMetadata().getName(), is(CruiseControlResources.deploymentName(cluster)));
        assertThat(dep.getMetadata().getNamespace(), is(namespace));
        assertThat(dep.getMetadata().getOwnerReferences().size(), is(1));
        assertThat(dep.getMetadata().getOwnerReferences().get(0), is(cc.createOwnerReference()));

        // checks on the main Cruise Control container
        Container ccContainer = containers.stream().filter(container -> ccImage.equals(container.getImage())).findFirst().get();
        assertThat(ccContainer.getImage(), is(cc.image));
        assertThat(ccContainer.getEnv(), is(getExpectedEnvVars()));
        assertThat(ccContainer.getPorts().size(), is(1));
        assertThat(ccContainer.getPorts().get(0).getName(), is(CruiseControl.REST_API_PORT_NAME));
        assertThat(ccContainer.getPorts().get(0).getProtocol(), is("TCP"));
        assertThat(dep.getSpec().getStrategy().getType(), is("RollingUpdate"));

        // Test volumes
        List<Volume> volumes = dep.getSpec().getTemplate().getSpec().getVolumes();
        assertThat(volumes.size(), is(3));

        Volume volume = volumes.stream().filter(vol -> CruiseControl.TLS_SIDECAR_CC_CERTS_VOLUME_NAME.equals(vol.getName())).findFirst().get();
        assertThat(volume, is(notNullValue()));
        assertThat(volume.getSecret().getSecretName(), is(CruiseControl.secretName(cluster)));

        volume = volumes.stream().filter(vol -> CruiseControl.TLS_SIDECAR_CA_CERTS_VOLUME_NAME.equals(vol.getName())).findFirst().get();
        assertThat(volume, is(notNullValue()));
        assertThat(volume.getSecret().getSecretName(), is(AbstractModel.clusterCaCertSecretName(cluster)));

        volume = volumes.stream().filter(vol -> CruiseControl.LOG_AND_METRICS_CONFIG_VOLUME_NAME.equals(vol.getName())).findFirst().get();
        assertThat(volume, is(notNullValue()));
        assertThat(volume.getConfigMap().getName(), is(CruiseControl.metricAndLogConfigsName(cluster)));

        // Test volume mounts
        List<VolumeMount> volumesMounts = dep.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
        assertThat(volumesMounts.size(), is(3));

        VolumeMount volumeMount = volumesMounts.stream().filter(vol -> CruiseControl.TLS_SIDECAR_CC_CERTS_VOLUME_NAME.equals(vol.getName())).findFirst().get();
        assertThat(volumeMount, is(notNullValue()));
        assertThat(volumeMount.getMountPath(), is(CruiseControl.TLS_SIDECAR_CC_CERTS_VOLUME_MOUNT));

        volumeMount = volumesMounts.stream().filter(vol -> CruiseControl.TLS_SIDECAR_CA_CERTS_VOLUME_NAME.equals(vol.getName())).findFirst().get();
        assertThat(volumeMount, is(notNullValue()));
        assertThat(volumeMount.getMountPath(), is(CruiseControl.TLS_SIDECAR_CA_CERTS_VOLUME_MOUNT));

        volumeMount = volumesMounts.stream().filter(vol -> CruiseControl.LOG_AND_METRICS_CONFIG_VOLUME_NAME.equals(vol.getName())).findFirst().get();
        assertThat(volumeMount, is(notNullValue()));
        assertThat(volumeMount.getMountPath(), is(CruiseControl.LOG_AND_METRICS_CONFIG_VOLUME_MOUNT));
    }

    @Test
    public void testEnvVars()   {
        assertThat(cc.getEnvVars(), is(getExpectedEnvVars()));
    }

    @Test
    public void testImagePullPolicy() {
        Deployment dep = cc.generateDeployment(null, true, ImagePullPolicy.ALWAYS, null);
        assertThat(dep.getSpec().getTemplate().getSpec().getContainers().get(0).getImagePullPolicy(), is(ImagePullPolicy.ALWAYS.toString()));

        dep = cc.generateDeployment(null, true, ImagePullPolicy.IFNOTPRESENT, null);
        assertThat(dep.getSpec().getTemplate().getSpec().getContainers().get(0).getImagePullPolicy(), is(ImagePullPolicy.IFNOTPRESENT.toString()));
    }

    @Test
    public void testContainerTemplateEnvVars() {
        ContainerEnvVar envVar1 = new ContainerEnvVar();
        String testEnvOneKey = "TEST_ENV_1";
        String testEnvOneValue = "test.env.one";
        envVar1.setName(testEnvOneKey);
        envVar1.setValue(testEnvOneValue);

        ContainerEnvVar envVar2 = new ContainerEnvVar();
        String testEnvTwoKey = "TEST_ENV_2";
        String testEnvTwoValue = "test.env.two";
        envVar2.setName(testEnvTwoKey);
        envVar2.setValue(testEnvTwoValue);

        CruiseControlSpec cruiseControlSpec = new CruiseControlSpecBuilder()
                .withImage(ccImage)
                .withNewTemplate()
                    .withNewCruiseControlContainer()
                       .withEnv(envVar1, envVar2)
                    .endCruiseControlContainer()
                .endTemplate()
                .build();

        Kafka resource = ResourceUtils.createKafkaCluster(namespace, cluster, replicas, image,
                healthDelay, healthTimeout, metricsCm, kafkaConfig, zooConfig,
                kafkaStorage, zkStorage, null, kafkaLogJson, zooLogJson, null, cruiseControlSpec);
        CruiseControl cc = CruiseControl.fromCrd(resource, VERSIONS);

        List<EnvVar> kafkaEnvVars = cc.getEnvVars();
        assertThat(kafkaEnvVars.stream().filter(var -> testEnvOneKey.equals(var.getName())).map(EnvVar::getValue).findFirst().get(), is(testEnvOneValue));
        assertThat(kafkaEnvVars.stream().filter(var -> testEnvTwoKey.equals(var.getName())).map(EnvVar::getValue).findFirst().get(), is(testEnvTwoValue));
    }

    @Test
    public void testContainerTemplateEnvVarsWithKeyConflict() {
        ContainerEnvVar envVar1 = new ContainerEnvVar();
        String testEnvOneKey = "TEST_ENV_1";
        String testEnvOneValue = "test.env.one";
        envVar1.setName(testEnvOneKey);
        envVar1.setValue(testEnvOneValue);

        ContainerEnvVar envVar2 = new ContainerEnvVar();
        String testEnvTwoKey = "TEST_ENV_2";
        String testEnvTwoValue = "my-special-value";
        envVar2.setName(testEnvTwoKey);
        envVar2.setValue(testEnvTwoValue);

        CruiseControlSpec cruiseControlSpec = new CruiseControlSpecBuilder()
                .withImage(ccImage)
                .withNewTemplate()
                    .withNewCruiseControlContainer()
                        .withEnv(envVar1, envVar2)
                    .endCruiseControlContainer()
                .endTemplate()
                .build();

        Kafka resource = ResourceUtils.createKafkaCluster(namespace, cluster, replicas, image,
                healthDelay, healthTimeout, metricsCm, kafkaConfig, zooConfig,
                kafkaStorage, zkStorage, null, kafkaLogJson, zooLogJson, null, cruiseControlSpec);
        CruiseControl cc = CruiseControl.fromCrd(resource, VERSIONS);

        List<EnvVar> kafkaEnvVars = cc.getEnvVars();
        assertThat(kafkaEnvVars.stream().filter(var -> testEnvOneKey.equals(var.getName())).map(EnvVar::getValue).findFirst().get(), is(testEnvOneValue));
    }

    @Test
    public void testCruiseControlNotDeployed() {
        Kafka resource = ResourceUtils.createKafkaCluster(namespace, cluster, replicas, image,
                healthDelay, healthTimeout, metricsCm, kafkaConfig, zooConfig,
                kafkaStorage, zkStorage, null, kafkaLogJson, zooLogJson, null,  null);
        CruiseControl cc = CruiseControl.fromCrd(resource, VERSIONS);

        assertThat(cc.generateDeployment(null, true, null, null), is(nullValue()));
        assertThat(cc.generateService(), is(nullValue()));
        assertThat(cc.generateSecret(null, true), is(nullValue()));
    }

    @Test
    public void testGenerateService()   {
        Service svc = cc.generateService();

        assertThat(svc.getSpec().getType(), is("ClusterIP"));
        assertThat(svc.getMetadata().getLabels(), is(expectedLabels(cc.getServiceName())));
        assertThat(svc.getSpec().getSelector(), is(expectedSelectorLabels()));
        assertThat(svc.getSpec().getPorts().size(), is(1));
        assertThat(svc.getSpec().getPorts().get(0).getName(), is(CruiseControl.REST_API_PORT_NAME));
        assertThat(svc.getSpec().getPorts().get(0).getPort(), is(new Integer(CruiseControl.REST_API_PORT)));
        assertThat(svc.getSpec().getPorts().get(0).getProtocol(), is("TCP"));

        checkOwnerReference(cc.createOwnerReference(), svc);
    }

    @Test
    public void testGenerateServiceWhenDisabled()   {
        Kafka resource = ResourceUtils.createKafkaCluster(namespace, cluster, replicas, image,
                healthDelay, healthTimeout, metricsCm, kafkaConfig, zooConfig,
                kafkaStorage, zkStorage, null, kafkaLogJson, zooLogJson, null, null);
        CruiseControl cc = CruiseControl.fromCrd(resource, VERSIONS);

        assertThat(cc.generateService(), is(nullValue()));
    }

    @Test
    public void testTemplate() {
        Map<String, String> depLabels = TestUtils.map("l1", "v1", "l2", "v2");
        Map<String, String> depAnots = TestUtils.map("a1", "v1", "a2", "v2");

        Map<String, String> podLabels = TestUtils.map("l3", "v3", "l4", "v4");
        Map<String, String> podAnots = TestUtils.map("a3", "v3", "a4", "v4");

        Map<String, String> svcLabels = TestUtils.map("l5", "v5", "l6", "v6");
        Map<String, String> svcAnots = TestUtils.map("a5", "v5", "a6", "v6");

        Kafka resource = new KafkaBuilder(ResourceUtils.createKafkaCluster(namespace, cluster, replicas, image, healthDelay, healthTimeout))
                .editSpec()
                    .withNewCruiseControl()
                        .withImage(ccImage)
                        .withNewTemplate()
                            .withNewDeployment()
                                .withNewMetadata()
                                    .withLabels(depLabels)
                                    .withAnnotations(depAnots)
                                .endMetadata()
                            .endDeployment()
                            .withNewPod()
                                .withNewMetadata()
                                    .withLabels(podLabels)
                                    .withAnnotations(podAnots)
                                .endMetadata()
                                .withNewPriorityClassName("top-priority")
                                .withNewSchedulerName("my-scheduler")
                            .endPod()
                            .withNewApiService()
                                .withNewMetadata()
                                    .withLabels(svcLabels)
                                    .withAnnotations(svcAnots)
                                .endMetadata()
                            .endApiService()
                        .endTemplate()
                    .endCruiseControl()
                .endSpec()
                .build();

        CruiseControl cc = CruiseControl.fromCrd(resource, VERSIONS);

        // Check Deployment
        Deployment dep = cc.generateDeployment(depAnots, true, null, null);
        assertThat(dep.getMetadata().getLabels().entrySet().containsAll(depLabels.entrySet()), is(true));
        assertThat(dep.getMetadata().getAnnotations().entrySet().containsAll(depAnots.entrySet()), is(true));

        // Check Pods
        assertThat(dep.getSpec().getTemplate().getMetadata().getLabels().entrySet().containsAll(podLabels.entrySet()), is(true));
        assertThat(dep.getSpec().getTemplate().getMetadata().getAnnotations().entrySet().containsAll(podAnots.entrySet()), is(true));
        assertThat(dep.getSpec().getTemplate().getSpec().getPriorityClassName(), is("top-priority"));
        assertThat(dep.getSpec().getTemplate().getSpec().getSchedulerName(), is("my-scheduler"));

        // Check Service
        Service svc = cc.generateService();
        assertThat(svc.getMetadata().getLabels().entrySet().containsAll(svcLabels.entrySet()), is(true));
        assertThat(svc.getMetadata().getAnnotations().entrySet().containsAll(svcAnots.entrySet()), is(true));
    }

    @AfterAll
    public static void cleanUp() {
        ResourceUtils.cleanUpTemporaryTLSFiles();
    }
}