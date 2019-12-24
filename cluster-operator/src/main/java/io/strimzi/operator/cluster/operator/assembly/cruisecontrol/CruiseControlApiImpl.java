/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class CruiseControlApiImpl implements CruiseControlApi {

    private static final Logger log = LogManager.getLogger(CruiseControlApiImpl.class.getName());

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

        String path = new PathBuilder(CruiseControlEndpoints.STATE)
                .addParameter(CruiseControlParameters.JSON, "true").build();

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
                        result.fail(new CruiseControlRestException(
                                "Unexpected status code " + response.statusCode() + " for GET request to " +
                                host + ":" + port + path));
                    }
                })
                .exceptionHandler(result::fail)
                .end();

        return result.future();
    }

    @Override
    public Future<CruiseControlResponse> rebalance(RebalanceOptions rbOptions) {
        return rebalance(rbOptions, null);
    }

    @SuppressWarnings("deprecation")
    public Future<CruiseControlResponse> rebalance(RebalanceOptions rbOptions, String userTaskId) {

        if (rbOptions == null && userTaskId == null) {
            return Future.factory.failedFuture(
                    new IllegalArgumentException("Either rebalance options or user task ID should be supplied, both were null"));
        }

        Promise<CruiseControlResponse> result = Promise.promise();
        HttpClientOptions httpOptions = new HttpClientOptions().setLogActivity(true);

        String path = new PathBuilder(CruiseControlEndpoints.REBALANCE)
                .addParameter(CruiseControlParameters.JSON, "true")
                .addRebalanceParameters(rbOptions)
                .build();


        HttpClientRequest request = vertx.createHttpClient(httpOptions)
                .post(port, host, path, response -> {
                    response.exceptionHandler(result::fail);
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        response.bodyHandler(buffer -> {
                            String returnedUTID = response.getHeader(USER_ID_HEADER);
                            JsonObject json = buffer.toJsonObject();
                            CruiseControlResponse ccResponse = new CruiseControlResponse(returnedUTID, json);
                            result.complete(ccResponse);
                        });
                    } else {
                        result.fail(new CruiseControlRestException(
                                "Unexpected status code " + response.statusCode() + " for POST request to " +
                                host + ":" + port + path));
                    }
                })
                .exceptionHandler(result::fail);

        if (userTaskId != null) {
            request.putHeader(USER_ID_HEADER, userTaskId);
        }

        request.end();

        return result.future();
    }

    @Override
    @SuppressWarnings("deprecation")
    public Future<CruiseControlResponse> getUserTaskStatus(String userTaskId) {

        Promise<CruiseControlResponse> result = Promise.promise();
        HttpClientOptions options = new HttpClientOptions().setLogActivity(true);

        String path = new PathBuilder(CruiseControlEndpoints.USER_TASKS)
                        .addParameter(CruiseControlParameters.JSON, "true")
                        .addParameter(CruiseControlParameters.FETCH_COMPLETE, "true")
                        .addParameter(CruiseControlParameters.USER_TASK_IDS, userTaskId).build();


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
                        result.fail(new CruiseControlRestException(
                                "Unexpected status code " + response.statusCode() + " for GET request to " +
                                host + ":" + port + path));
                    }
                })
                .exceptionHandler(result::fail)
                .end();

        return result.future();
    }

    @Override
    @SuppressWarnings("deprecation")
    public Future<CruiseControlResponse> stopExecution() {

        Promise<CruiseControlResponse> result = Promise.promise();
        HttpClientOptions options = new HttpClientOptions().setLogActivity(true);

        String path = new PathBuilder(CruiseControlEndpoints.STOP)
                        .addParameter(CruiseControlParameters.JSON, "true").build();

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
                        result.fail(new CruiseControlRestException(
                                "Unexpected status code " + response.statusCode()  + " for GET request to " +
                                host + ":" + port + path));
                    }
                })
                .exceptionHandler(result::fail)
                .end();

        return result.future();
    }
}
