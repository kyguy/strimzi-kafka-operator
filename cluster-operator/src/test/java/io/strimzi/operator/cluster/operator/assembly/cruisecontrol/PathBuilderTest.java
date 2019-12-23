/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class PathBuilderTest {

    private static final String DEFAULT_QUERY = "?" +
            CruiseControlParameters.DRY_RUN.key + "=true" + "&" +
            CruiseControlParameters.JSON.key + "=true" + "&" +
            CruiseControlParameters.VERBOSE.key + "=false";
    @Test
    public void testQueryStringPair() {

        String path = new PathBuilder(CruiseControlEndpoints.STATE)
                .addParameter(CruiseControlParameters.DRY_RUN, "true")
                .addParameter(CruiseControlParameters.JSON, "true")
                .addParameter(CruiseControlParameters.VERBOSE, "false")
                .build();

        assertThat(path, containsString(DEFAULT_QUERY));

    }

    @Test
    public void testQueryStringList() {

        List<String> goals = new ArrayList();
        goals.add("goal.one");
        goals.add("goal.two");
        goals.add("goal.three");
        goals.add("goal.four");
        goals.add("goal.five");

        StringBuilder expectedQuery = new StringBuilder(DEFAULT_QUERY + "&" + CruiseControlParameters.GOALS.key + "=");

        for (int i = 0; i < goals.size(); i++) {
            expectedQuery.append(goals.get(i));
            if (i < goals.size() - 1) {
                expectedQuery.append(",");
            }
        }

        String path = new PathBuilder(CruiseControlEndpoints.REBALANCE)
                .addParameter(CruiseControlParameters.DRY_RUN, "true")
                .addParameter(CruiseControlParameters.JSON, "true")
                .addParameter(CruiseControlParameters.VERBOSE, "false")
                .addParameter(CruiseControlParameters.GOALS, goals)
                .build();

        assertThat(path, containsString(expectedQuery.toString()));

    }
}
