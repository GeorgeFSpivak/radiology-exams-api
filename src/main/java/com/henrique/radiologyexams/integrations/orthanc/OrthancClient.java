package com.henrique.radiologyexams.integrations.orthanc;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;


@Component
public class OrthancClient {
    private final RestClient restClient;

    public OrthancClient(
            @Qualifier("orthancRestClient") RestClient restClient
    ){
        this.restClient = restClient;
    }

    public OrthancSystemResponseDTO getSystem(){
        OrthancSystemResponseDTO response = restClient.get()
                .uri("/system")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(OrthancSystemResponseDTO.class);

        if (response == null) {
            throw new RestClientException("Orthanc returned an empty system response.");
        }

        return response;
    }

    public OrthancUploadResponseDTO uploadInstance(Resource dicomFile)  {
        OrthancUploadResponseDTO response = restClient.post()
                .uri("/instances")
                .contentType(MediaType.parseMediaType("application/dicom"))
                .accept(MediaType.APPLICATION_JSON)
                .body(dicomFile)
                .retrieve()
                .body(OrthancUploadResponseDTO.class);

        if (response == null) {
            throw new RestClientException("Orthanc returned an empty upload response.");
        }

        if (!"Success".equals(response.status()) && !"AlreadyStored".equals(response.status())) {
            throw new RestClientException("Orthanc did not store the dicom instance.");
        }

        if (response.instanceId() == null || response.instanceId().isBlank()
                || response.parentStudyId() == null || response.parentStudyId().isBlank()) {
            throw new RestClientException("Orthanc returned an upload response with missing identifiers.");
        }

        return response;
    }
}
