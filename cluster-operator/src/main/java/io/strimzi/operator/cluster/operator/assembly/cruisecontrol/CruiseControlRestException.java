package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

/**
 * Represents an exception related to Cruise Control REST API interaction
 */
class CruiseControlRestException extends RuntimeException {
    public CruiseControlRestException(String message) {
        super(message);
    }
}
