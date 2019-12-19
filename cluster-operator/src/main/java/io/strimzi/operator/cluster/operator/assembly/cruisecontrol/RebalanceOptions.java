package io.strimzi.operator.cluster.operator.assembly.cruisecontrol;

import java.util.List;

public class RebalanceOptions {

    private boolean isDryRun;
    private List<String> goals;
    private boolean verbose;
    private static final boolean replyWithJson = true;

    public boolean isDryRun() {
        return isDryRun;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public List<String> getGoals() {
        return goals;
    }

    public String getQueryString() {
        String queryString = "?" +
                CruiseControlParameters.DRY_RUN.asPair(String.valueOf(isDryRun)) + "&" +
                CruiseControlParameters.JSON.asPair(String.valueOf(replyWithJson)) + "&" +
                CruiseControlParameters.VERBOSE.asPair(String.valueOf(verbose));

        if (goals != null) {
            queryString += "&" + CruiseControlParameters.GOALS.asList(goals) ;
        }

        return queryString;
    }

    private RebalanceOptions(RebalanceOptionsBuilder builder) {
        this.isDryRun = builder.isDryRun;
        this.goals = builder.goals;
    }

    public static class RebalanceOptionsBuilder {

        private boolean isDryRun;
        private boolean verbose;
        private List<String> goals;

        public RebalanceOptionsBuilder() {
            isDryRun = true;
            verbose = false;
            goals = null;
        }

        public RebalanceOptionsBuilder withFullRun() {
            this.isDryRun = false;
            return this;
        }

        public RebalanceOptionsBuilder withVerboseResponse() {
            this.verbose = true;
            return this;
        }

        public RebalanceOptionsBuilder withGoals(List<String> goals) {
            this.goals = goals;
            return this;
        }

        public RebalanceOptions build() {
            return new RebalanceOptions(this);
        }



    }

}
