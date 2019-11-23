/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.model;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LifecycleBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategyBuilder;
import io.fabric8.kubernetes.api.model.apps.RollingUpdateDeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudget;
import io.strimzi.api.kafka.model.ContainerEnvVar;
import io.strimzi.api.kafka.model.CruiseControlResources;
import io.strimzi.api.kafka.model.CruiseControlSpec;
import io.strimzi.api.kafka.model.EntityOperatorSpec;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaClusterSpec;
import io.strimzi.api.kafka.model.KafkaResources;
import io.strimzi.api.kafka.model.TlsSidecar;
import io.strimzi.operator.cluster.ClusterOperatorConfig;
import io.strimzi.operator.cluster.operator.assembly.KafkaAssemblyOperator;
import io.strimzi.operator.common.model.Labels;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.strimzi.operator.cluster.model.KafkaCluster.ENV_VAR_KAFKA_CONFIGURATION;

public class CruiseControl extends AbstractModel {

    private static final Logger log = LogManager.getLogger(KafkaAssemblyOperator.class.getName());

    protected static final String TLS_SIDECAR_NAME = "tls-sidecar";
    protected static final String TLS_SIDECAR_CC_CERTS_VOLUME_NAME = "cc-certs";
    protected static final String TLS_SIDECAR_CC_CERTS_VOLUME_MOUNT = "/etc/tls-sidecar/cc-certs/";
    protected static final String TLS_SIDECAR_CA_CERTS_VOLUME_NAME = "cluster-ca-certs";
    protected static final String TLS_SIDECAR_CA_CERTS_VOLUME_MOUNT = "/etc/tls-sidecar/cluster-ca-certs/";

    // Entity Operator configuration keys
    public static final String ENV_VAR_ZOOKEEPER_CONNECT = "STRIMZI_ZOOKEEPER_CONNECT";

    private String zookeeperConnect;
    private List<ContainerEnvVar> templateTlsSidecarContainerEnvVars;

    // Configuration defaults
    protected static final int DEFAULT_REPLICAS = 1;
    private TlsSidecar tlsSidecar;
    private String tlsSidecarImage;

    protected static final String CRUISE_CONTROL_PORT_NAME = "http-9090";
    protected static final int CRUISE_CONTROL_PORT = 9090;

    // Cruise Control configuration keys (EnvVariables)
    protected static final String ENV_VAR_PREFIX = "CRUISE_CONTROL_";

    protected List<ContainerEnvVar> templateContainerEnvVars;

    private boolean isDeployed;

    /**
     * Constructor
     *
     * @param namespace Kubernetes/OpenShift namespace where Cruise Control resources are going to be created
     * @param cluster   overall cluster name
     * @param labels    labels to add to the cluster
     */
    protected CruiseControl(String namespace, String cluster, Labels labels) {
        super(namespace, cluster, labels);
        this.name = CruiseControlResources.deploymentName(cluster);
        this.serviceName = CruiseControlResources.serviceName(cluster);
        //this.ancillaryConfigName = CruiseControlResources.metricsAndLogConfigMapName(cluster);
        this.replicas = DEFAULT_REPLICAS;

        this.mountPath = "/var/lib/kafka";
        this.logAndMetricsConfigVolumeName = "kafka-metrics-and-logging";
        this.logAndMetricsConfigMountPath = "/opt/kafka/custom-config/";
        this.zookeeperConnect = defaultZookeeperConnect(cluster);
    }

    protected void setTlsSidecar(TlsSidecar tlsSidecar) {
        this.tlsSidecar = tlsSidecar;
    }

    protected static String defaultZookeeperConnect(String cluster) {
        return ZookeeperCluster.serviceName(cluster) + ":" + EntityOperatorSpec.DEFAULT_ZOOKEEPER_PORT;
    }

    public void setZookeeperConnect(String zookeeperConnect) {
        this.zookeeperConnect = zookeeperConnect;
    }

    public String getZookeeperConnect() {
        return zookeeperConnect;
    }

    public static CruiseControl fromCrd(Kafka kafkaAssembly, KafkaVersion.Lookup versions) {
        CruiseControl cruiseControl = new CruiseControl(kafkaAssembly.getMetadata().getNamespace(),
                    kafkaAssembly.getMetadata().getName(),
                    Labels.fromResource(kafkaAssembly).withKind(kafkaAssembly.getKind()));

        EntityOperatorSpec entityOperatorSpec  = kafkaAssembly.getSpec().getEntityOperator();

        if (entityOperatorSpec != null &&
            entityOperatorSpec.getCruiseControlOperator() != null &&
            entityOperatorSpec.getCruiseControlOperator().getCruiseControl() != null) {
            CruiseControlSpec spec = kafkaAssembly.getSpec().getEntityOperator().getCruiseControlOperator().getCruiseControl();

            cruiseControl.isDeployed = true;

            cruiseControl.setReplicas(spec != null && spec.getReplicas() > 0 ? spec.getReplicas() : DEFAULT_REPLICAS);
            cruiseControl.setImage(versions.kafkaImage(spec.getImage(), spec.getVersion()));

            TlsSidecar tlsSidecar = spec.getTlsSidecar();

            cruiseControl.setTlsSidecar(tlsSidecar);

            String tlsSideCarImage = tlsSidecar != null ? tlsSidecar.getImage() : null;
            if (tlsSideCarImage == null) {
                KafkaClusterSpec kafkaClusterSpec = kafkaAssembly.getSpec().getKafka();
                tlsSideCarImage = System.getenv().getOrDefault(ClusterOperatorConfig.STRIMZI_DEFAULT_TLS_SIDECAR_CRUISE_CONTROL_IMAGE, versions.kafkaImage(kafkaClusterSpec.getImage(), versions.defaultVersion().version()));
            }
            cruiseControl.tlsSidecarImage = tlsSideCarImage;
            cruiseControl.setOwnerReference(kafkaAssembly);
        } else {
            cruiseControl.isDeployed = false;
        }

        return cruiseControl;
    }

    public static StatefulSet updateKafkaConfig(Kafka kafkaAssembly, StatefulSet kafkaSs) {

        Map<String, String> env = ModelUtils.getKafkaContainerEnv(kafkaSs);
        KafkaConfiguration currentKafkaConfig = KafkaConfiguration.unvalidated(env.get(ENV_VAR_KAFKA_CONFIGURATION));

        EntityOperatorSpec entityOperatorSpec  = kafkaAssembly.getSpec().getEntityOperator();

        if (entityOperatorSpec != null &&
            entityOperatorSpec.getCruiseControlOperator() != null &&
            entityOperatorSpec.getCruiseControlOperator().getCruiseControl() != null) {
            currentKafkaConfig.setConfigOption("metric.reporters", "com.linkedin.kafka.cruisecontrol.metricsreporter.CruiseControlMetricsReporter");
        } else {
            currentKafkaConfig.removeConfigOption("metric.reporters");
        }
        env.put(ENV_VAR_KAFKA_CONFIGURATION, currentKafkaConfig.getConfiguration());
        kafkaSs.getSpec().getTemplate().getSpec().getContainers().get(0).setEnv(ModelUtils.envAsList(env));

        return kafkaSs;
    }

    public static String cruiseControlName(String cluster) {
        return KafkaResources.cruiseControlDeploymentName(cluster);
    }

    public static String cruiseControlServiceName(String cluster) {
        return KafkaResources.cruiseControlDeploymentName(cluster);
    }

    public Service generateService() {
        if (!isDeployed()) {
            return null;
        }

        List<ServicePort> ports = new ArrayList<>(1);
        ports.add(createServicePort(CRUISE_CONTROL_PORT_NAME, CRUISE_CONTROL_PORT, CRUISE_CONTROL_PORT, "TCP"));
        Map<String, String> annotations = new HashMap<String, String>(1);
        annotations.put("cruisecontrol/port", String.valueOf(CRUISE_CONTROL_PORT));

        return createService("ClusterIP", ports, annotations);
    }

    protected List<ContainerPort> getContainerPortList() {
        List<ContainerPort> portList = new ArrayList<>(1);
        if (isMetricsEnabled) {
            portList.add(createContainerPort(METRICS_PORT_NAME, METRICS_PORT, "TCP"));
        }

        return portList;
    }

    protected List<Volume> getVolumes(boolean isOpenShift) {
        List<Volume> volumeList = new ArrayList<>(1);
        volumeList.add(createSecretVolume(TLS_SIDECAR_CC_CERTS_VOLUME_NAME, CruiseControl.secretName(cluster), isOpenShift));
        volumeList.add(createSecretVolume(TLS_SIDECAR_CA_CERTS_VOLUME_NAME, AbstractModel.clusterCaCertSecretName(cluster), isOpenShift));

        return volumeList;
    }

    protected List<VolumeMount> getVolumeMounts() {
        List<VolumeMount> volumeMountList = new ArrayList<>(1);
        //volumeMountList.add(createVolumeMount(logAndMetricsConfigVolumeName, logAndMetricsConfigMountPath));
        volumeMountList.add(createVolumeMount(CruiseControl.TLS_SIDECAR_CC_CERTS_VOLUME_NAME, CruiseControl.TLS_SIDECAR_CC_CERTS_VOLUME_MOUNT));
        volumeMountList.add(createVolumeMount(CruiseControl.TLS_SIDECAR_CA_CERTS_VOLUME_NAME, CruiseControl.TLS_SIDECAR_CA_CERTS_VOLUME_MOUNT));

        return volumeMountList;
    }

    public Deployment generateDeployment(Map<String, String> annotations, boolean isOpenShift, ImagePullPolicy imagePullPolicy, List<LocalObjectReference> imagePullSecrets) {
        if (!isDeployed()) {
            return null;
        }

        DeploymentStrategy updateStrategy = new DeploymentStrategyBuilder()
                .withType("RollingUpdate")
                .withRollingUpdate(new RollingUpdateDeploymentBuilder()
                        .withMaxSurge(new IntOrString(1))
                        .withMaxUnavailable(new IntOrString(0))
                        .build())
                .build();

        return createDeployment(
                updateStrategy,
                Collections.emptyMap(),
                annotations,
                getMergedAffinity(),
                getInitContainers(imagePullPolicy),
                getContainers(imagePullPolicy),
                getVolumes(isOpenShift),
                imagePullSecrets);
    }

    @Override
    protected List<Container> getContainers(ImagePullPolicy imagePullPolicy) {

        List<Container> containers = new ArrayList<>();
        Container container = new ContainerBuilder()
                .withName(name)
                .withImage(getImage())
                .withCommand("/opt/cruise-control/cruise_control_run.sh")
                .withEnv(getEnvVars())
                .withPorts(getContainerPortList())
                .withResources(getResources())
                .withVolumeMounts(getVolumeMounts())
                .withImagePullPolicy(determineImagePullPolicy(imagePullPolicy, getImage()))
                .build();

        String tlsSidecarImage = this.tlsSidecarImage;
        if (tlsSidecar != null && tlsSidecar.getImage() != null) {
            tlsSidecarImage = tlsSidecar.getImage();
        }

        Container tlsSidecarContainer = new ContainerBuilder()
                .withName(TLS_SIDECAR_NAME)
                .withImage(tlsSidecarImage)
                .withCommand("/opt/stunnel/cruise_control_stunnel_run.sh")
                .withLivenessProbe(ModelUtils.tlsSidecarLivenessProbe(tlsSidecar))
                .withReadinessProbe(ModelUtils.tlsSidecarReadinessProbe(tlsSidecar))
                .withResources(tlsSidecar != null ? tlsSidecar.getResources() : null)
                .withEnv(getTlsSidecarEnvVars())
                .withVolumeMounts(createVolumeMount(TLS_SIDECAR_CC_CERTS_VOLUME_NAME, TLS_SIDECAR_CC_CERTS_VOLUME_MOUNT),
                        createVolumeMount(TLS_SIDECAR_CA_CERTS_VOLUME_NAME, TLS_SIDECAR_CA_CERTS_VOLUME_MOUNT))
                .withLifecycle(new LifecycleBuilder().withNewPreStop().withNewExec()
                        .withCommand("/opt/stunnel/cruise_control_stunnel_pre_stop.sh",
                                String.valueOf(templateTerminationGracePeriodSeconds))
                        .endExec().endPreStop().build())
                .withImagePullPolicy(determineImagePullPolicy(imagePullPolicy, tlsSidecarImage))
                .build();

        containers.add(container);
        containers.add(tlsSidecarContainer);

        return containers;
    }

    @Override
    protected List<EnvVar> getEnvVars() {
        List<EnvVar> varList = new ArrayList<>();

        varList.add(buildEnvVar(ENV_VAR_STRIMZI_KAFKA_GC_LOG_ENABLED, String.valueOf(gcLoggingEnabled)));

        heapOptions(varList, 1.0, 0L);
        jvmPerformanceOptions(varList);

        addContainerEnvsToExistingEnvs(varList, templateContainerEnvVars);

        return varList;
    }

    /**
     * Generates the PodDisruptionBudget.
     *
     * @return The PodDisruptionBudget.
     */
    public PodDisruptionBudget generatePodDisruptionBudget() {
        return createPodDisruptionBudget();
    }

    @Override
    protected String getDefaultLogConfigFileName() {
        return "cruiseControlDefaultLoggingProperties";
    }

    @Override
    protected String getServiceAccountName() {
        return CruiseControlResources.serviceAccountName(cluster);
    }

    /**
     * Get the name of the Cruise Control service account given the name of the {@code cluster}.
     * @param cluster The cluster name
     * @return The name of the Cruise Control service account.
     */
    public static String cruiseControlServiceAccountName(String cluster) {
        return CruiseControlResources.serviceAccountName(cluster);
    }

    protected List<EnvVar> getTlsSidecarEnvVars() {
        List<EnvVar> varList = new ArrayList<>();
        varList.add(ModelUtils.tlsSidecarLogEnvVar(tlsSidecar));
        varList.add(buildEnvVar(ENV_VAR_ZOOKEEPER_CONNECT, zookeeperConnect));

        addContainerEnvsToExistingEnvs(varList, templateTlsSidecarContainerEnvVars);

        return varList;
    }

    /**
     * Generates the name of the Kafka Exporter secret with certificates for connecting to Kafka brokers
     *
     * @param kafkaCluster  Name of the Kafka Custom Resource
     * @return  Name of the Kafka Exporter secret
     */
    public static String secretName(String kafkaCluster) {
        return CruiseControlResources.secretName(kafkaCluster);
    }

    /**
     * Returns whether the Cruise Control is enabled or not
     *
     * @return True if Cruise Control is enabled. False otherwise.
     */
    private boolean isDeployed() {
        return isDeployed;
    }

    /**
     * Generate the Secret containing the Cruise Control certificate signed by the cluster CA certificate used for TLS based
     * internal communication with Kafka and Zookeeper.
     * It also contains the related Cruise Control private key.
     *
     * @param clusterCa The cluster CA.
     * @return The generated Secret.
     */
    public Secret generateSecret(ClusterCa clusterCa) {
        if (!isDeployed()) {
            return null;
        }
        Secret secret = clusterCa.cruiseControlSecret();
        return ModelUtils.buildSecret(clusterCa, secret, namespace, CruiseControl.secretName(cluster), name, "cruise-control", labels, createOwnerReference());
    }

}
