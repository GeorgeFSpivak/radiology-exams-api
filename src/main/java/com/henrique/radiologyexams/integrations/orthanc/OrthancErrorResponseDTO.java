package com.henrique.radiologyexams.integrations.orthanc;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrthancErrorResponseDTO(
        @JsonProperty("OrthancStatus") Integer orthancStatus
){
}
