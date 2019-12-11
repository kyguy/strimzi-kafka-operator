/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.strimzi.operator.cluster.model;

import io.strimzi.api.kafka.model.CruiseControlSpec;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Class for handling Cruise Control configuration passed by the user
 */
public class CruiseControlConfiguration extends AbstractConfiguration {
    private static final List<String> FORBIDDEN_OPTIONS;
    private static final List<String> EXCEPTIONS;

    static {
        FORBIDDEN_OPTIONS = asList(CruiseControlSpec.FORBIDDEN_PREFIXES.split(", "));
        EXCEPTIONS = asList(CruiseControlSpec.FORBIDDEN_PREFIX_EXCEPTIONS.split(", "));
    }

    /**
     * Constructor used to instantiate this class from JsonObject. Should be used to create configuration from
     * ConfigMap / CRD.
     *
     * @param jsonOptions     Json object with configuration options as key ad value pairs.
     */
    public CruiseControlConfiguration(Iterable<Map.Entry<String, Object>> jsonOptions) {
        super(jsonOptions, FORBIDDEN_OPTIONS, EXCEPTIONS);
    }

    private CruiseControlConfiguration(String configuration, List<String> forbiddenOptions) {
        super(configuration, forbiddenOptions);
    }

    /**
     * Returns a CruiseControlConfiguration created without forbidden option filtering.
     * @param string A string representation of the Properties
     * @return The KafkaConfiguration
     */
    public static CruiseControlConfiguration unvalidated(String string) {
        return new CruiseControlConfiguration(string, emptyList());
    }
}
