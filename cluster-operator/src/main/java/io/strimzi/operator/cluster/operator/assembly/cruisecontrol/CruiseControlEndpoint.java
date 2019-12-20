/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
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
