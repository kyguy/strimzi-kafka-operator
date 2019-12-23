/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;

class CruiseControlApiImpl implements CruiseControlApi {

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

    public Future<CruiseControlResponse> getCruiseControlState() {
        return getCruiseControlState(false);
    }

    @Override
    @SuppressWarnings("deprecation")
    public Future<CruiseControlResponse> getCruiseControlState(boolean verbose) {

        Promise<CruiseControlResponse> result = Promise.promise();
        HttpClientOptions options = new HttpClientOptions().setLogActivity(true);

        String path = CruiseControlEndpoint.STATE.path + "?json=true";

        vertx.createHttpClient(options)
                .get(port, host, path, response -> {
                    response.exceptionHandler(result::fail);
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        String userTaskID = response.getHeader(USER_ID_HEADER);
                        response.bodyHandler(buffer -> {
                            JsonObject json = buffer.toJsonObject();
                            CruiseControlResponse ccResponse = new CruiseControlResponse(userTaskID, json);
                            result.complete(ccResponse);
                        });

                    } else {
                        result.fail(new CruiseControlRestException("Unexpected status code " + response.statusCode()
                                + " for GET request to " + host + ":" + port + CruiseControlEndpoint.STATE.path));
                    }
                })
                .exceptionHandler(result::fail)
                .end();

        return result.future();
    }

    @Override
    public Future<CruiseControlResponse> rebalance(RebalanceOptions options) {
        return Future.failedFuture("Not implemented yet");
    }

    @Override
    public Future<CruiseControlResponse> getUserTaskStatus(String userTaskId) {
        return Future.failedFuture("Not implemented yet");
    }

    @Override
    @SuppressWarnings("deprecation")
    public Future<CruiseControlResponse> stopExecution() {

        Promise<CruiseControlResponse> result = Promise.promise();
        HttpClientOptions options = new HttpClientOptions().setLogActivity(true);

        String path = CruiseControlEndpoint.STOP.path + "?json=true";

        vertx.createHttpClient(options)
                .post(port, host, path, response -> {
                    response.exceptionHandler(result::fail);
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        String userTaskID = response.getHeader(USER_ID_HEADER);
                        response.bodyHandler(buffer -> {
                            JsonObject json = buffer.toJsonObject();
                            CruiseControlResponse ccResponse = new CruiseControlResponse(userTaskID, json);
                            result.complete(ccResponse);
                        });

                    } else {
                        result.fail("Unexpected status code " + response.statusCode()
                                + " for GET request to " + host + ":" + port + CruiseControlEndpoint.STOP.path);
                    }
                })
                .exceptionHandler(result::fail)
                .end();

        return result.future();
    }
}
