package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class RebalanceOptionsTest {

    private static final String DEFAULT_QUERY = "?" +
            CruiseControlParameters.DRY_RUN.key + "=true" + "&" +
            CruiseControlParameters.JSON.key + "=true" + "&" +
            CruiseControlParameters.VERBOSE.key + "=false";
    @Test
    public void testQueryStringPair() {

        RebalanceOptions rbOptions = new RebalanceOptions.RebalanceOptionsBuilder().build();

        assertThat(rbOptions.getQueryString(), is(DEFAULT_QUERY));

    }

    @Test
    public void testQueryStringList() {

        List<String> goals = new ArrayList();
        goals.add("goal.one");
        goals.add("goal.two");
        goals.add("goal.three");
        goals.add("goal.four");
        goals.add("goal.five");

        RebalanceOptions rbOptions = new RebalanceOptions.RebalanceOptionsBuilder().withGoals(goals).build();

        StringBuilder expectedQuery = new StringBuilder(DEFAULT_QUERY);

        expectedQuery.append("&").append(CruiseControlParameters.GOALS.key).append("=").append(goals.get(0));
        for(int i=1;i < goals.size(); i++) {
            expectedQuery.append(",").append(goals.get(i));
        }

        assertThat(rbOptions.getQueryString(), is(expectedQuery.toString()));

    }
}
