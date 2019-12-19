package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

public enum CruiseControlParameters {

    DRY_RUN("dryrun"),
    JSON("json"),
    GOALS("goals"),
    VERBOSE("verbose");

    String key;

    CruiseControlParameters(String key) {
       this.key = key;
    }

    public String asPair(String value) {
        return key + "=" + value;
    }

    public String asList(Iterable<String> values) {
        return key + "=" + String.join(",", values);
    }

}
