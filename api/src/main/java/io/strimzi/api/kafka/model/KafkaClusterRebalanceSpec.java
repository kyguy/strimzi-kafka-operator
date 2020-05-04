/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.strimzi.crdgenerator.annotations.Description;
import io.sundr.builder.annotations.Buildable;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "goals", "skipHardGoalCheck" })
@EqualsAndHashCode
public class KafkaClusterRebalanceSpec implements UnknownPropertyPreserving, Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> goals;
    private boolean skipHardGoalCheck;

    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @Description("A list of goals, ordered by decreasing priority, to use for generating and executing the rebalance proposal." +
            "The supported goals are available at https://github.com/linkedin/cruise-control#goals." +
            "If an empty goals list is provided, the goals declared in the default.goals Cruise Control configuration parameter are used.")
    public List<String> getGoals() {
        return goals;
    }

    public void setGoals(List<String> goals) {
        this.goals = goals;
    }

    @Description("Whether to allow hard goals to be skipped in rebalance proposal generation. Default is false.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public boolean isSkipHardGoalCheck() {
        return skipHardGoalCheck;
    }

    public void setSkipHardGoalCheck(boolean skipHardGoalCheck) {
        this.skipHardGoalCheck = skipHardGoalCheck;
    }

    @Override
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @Override
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        YAMLMapper mapper = new YAMLMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
