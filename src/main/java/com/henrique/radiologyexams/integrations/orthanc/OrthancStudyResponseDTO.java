package com.henrique.radiologyexams.integrations.orthanc;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public record OrthancStudyResponseDTO(
        @JsonProperty("MainDicomTags") MainDicomTags mainDicomTags
){
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainDicomTags(
            @JsonProperty("StudyInstanceUID") String studyInstanceUid
    ){
    }
}
