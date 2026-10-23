package com.henrique.radiologyexams.integrations.orthanc;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrthancSystemResponseDTO (
        @JsonProperty("Name") String name,
        @JsonProperty("Version") String version
){
}
