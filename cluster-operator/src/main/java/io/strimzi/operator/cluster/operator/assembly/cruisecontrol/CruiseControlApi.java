/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

import io.vertx.core.Future;

/**
 * Cruise Control REST API interface definition
 */
public interface CruiseControlApi {

    String USER_ID_HEADER = "User-Task-ID";

    Future<CruiseControlResponse> getCruiseControlState(boolean verbose);
    Future<CruiseControlResponse> rebalance(RebalanceOptions options);
    Future<CruiseControlResponse> getUserTaskStatus(String userTaskId);
    Future<CruiseControlResponse> stopExecution();

}

