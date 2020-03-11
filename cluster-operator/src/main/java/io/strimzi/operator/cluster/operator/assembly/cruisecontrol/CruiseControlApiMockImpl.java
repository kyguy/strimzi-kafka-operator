/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class CruiseControlApiMockImpl implements CruiseControlApi {

    private final Vertx vertx;
    private int count = 0;
    private int maxAttempts = 5;
    private boolean proposalReady = false;

    public CruiseControlApiMockImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public Future<CruiseControlResponse> getCruiseControlState(String host, int port, boolean verbose) {
        return null;
    }

    @Override
    public Future<CruiseControlResponse> rebalance(String host, int port, RebalanceOptions options) {
        CruiseControlResponse response;
        if (proposalReady) {
            response = new CruiseControlResponse("12345", new JsonObject().put("rebalance", new JsonObject().put("plan", "plan")));
        } else {
            response = new CruiseControlResponse("12345", null);
        }
        return Future.succeededFuture(response);
    }

    @Override
    public Future<CruiseControlResponse> getUserTaskStatus(String host, int port, String userTaskId) {
        CruiseControlResponse response;
        if (count++ < maxAttempts) {
            response = new CruiseControlResponse(userTaskId, new JsonObject().put("Status", "in-progress"));
        } else {
            response = new CruiseControlResponse(userTaskId, new JsonObject().put("Status", "completed").put("rebalance", new JsonObject().put("plan", "plan")));
            count = 0;
        }
        return Future.succeededFuture(response);
    }

    @Override
    public Future<CruiseControlResponse> stopExecution(String host, int port) {
        return Future.succeededFuture(null);
    }
}
