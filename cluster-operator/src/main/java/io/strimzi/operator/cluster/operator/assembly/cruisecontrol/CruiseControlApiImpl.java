/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class CruiseControlApiImpl implements CruiseControlApi {

    private final Vertx vertx;
    private String host;
    private int port;

    public CruiseControlApiImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    public CruiseControlApiImpl(Vertx vertx, String host, int port) {
        this(vertx);
        this.host = host;
        this.port = port;
    }

    @Override
    public Future<CruiseControlResponse> getCruiseControlState(boolean verbose) {
        return Future.failedFuture("Not yet implemented");
    }

    @Override
    public Future<CruiseControlResponse> rebalance(RebalanceOptions options) {
        return Future.failedFuture("Not yet implemented");
    }

    @Override
    public Future<CruiseControlResponse> getUserTaskStatus(String userTaskId) {
        return Future.failedFuture("Not yet implemented");
    }

    @Override
    public Future<CruiseControlResponse> stopExecution() {
        return Future.failedFuture("Not yet implemented");
    }
}
