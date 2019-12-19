package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

enum CruiseControlEndpoint {

    STATE("/kafkacruisecontrol/state"),
    REBALANCE("/kafkacruisecontrol/rebalance"),
    STOP("/kafkacruisecontrol/stop_proposal_execution");

    String path;

    CruiseControlEndpoint(String path) {
        this.path = path;
    }
}
