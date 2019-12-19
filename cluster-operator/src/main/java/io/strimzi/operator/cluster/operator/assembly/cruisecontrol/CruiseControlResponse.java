package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

import io.vertx.core.json.JsonObject;

public class CruiseControlResponse {

    private String userTaskId;
    private JsonObject json;

    CruiseControlResponse(String userTaskId, JsonObject json) {
        this.userTaskId = userTaskId;
        this.json = json;
    }

    String getUserTaskId() {
        return userTaskId;
    }

    JsonObject getJson() {
        return json;
    }

}
