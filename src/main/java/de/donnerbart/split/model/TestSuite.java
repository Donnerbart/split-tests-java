package de.donnerbart.split.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestSuite {

    @JsonProperty(required = true)
    private @NotNull String name;

    @JsonProperty(required = true)
    private double time;

    public @NotNull String getName() {
        return name;
    }

    public double getTime() {
        return time;
    }
}
