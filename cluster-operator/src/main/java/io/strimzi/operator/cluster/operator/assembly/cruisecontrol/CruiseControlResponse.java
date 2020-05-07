/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

import io.vertx.core.json.JsonObject;

public class CruiseControlResponse {

    private String userTaskId;
    private JsonObject json;
    private boolean notEnoughDataForProposal;

    CruiseControlResponse(String userTaskId, JsonObject json) {
        this.userTaskId = userTaskId;
        this.json = json;
        this.notEnoughDataForProposal = false;
    }

    public String getUserTaskId() {
        return userTaskId;
    }

    public JsonObject getJson() {
        return json;
    }

    public String prettyPrint() {
        return "User Task ID: " + userTaskId + "\nJSON:\n " + json.encodePrettily();
    }

    public String toString() {
        return "User Task ID: " + userTaskId + " JSON: " + json.toString();
    }

    public boolean thereIsNotEnoughDataForProposal() {
        return this.notEnoughDataForProposal;
    }

    public void setNotEnoughDataForProposal(boolean notEnoughData) {
        this.notEnoughDataForProposal = notEnoughData;
    }

}
