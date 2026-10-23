package com.henrique.radiologyexams.integrations.orthanc;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public record OrthancUploadResponseDTO (
        @JsonProperty("ID") String instanceId,
        @JsonProperty("ParentStudy") String parentStudyId,
        @JsonProperty("Status") String status
){
}
