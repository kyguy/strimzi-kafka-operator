package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class CruiseControlApiImpl implements CruiseControlApi {

    private final Vertx vertx;
    private final String host;
    private final int port;

    private static final String USER_ID_HEADER = "User-Task-ID";

    public CruiseControlApiImpl(Vertx vertx, String host, int port) {
        this.vertx = vertx;
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
