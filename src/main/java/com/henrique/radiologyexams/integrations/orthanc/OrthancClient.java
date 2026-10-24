package com.henrique.radiologyexams.integrations.orthanc;


import com.henrique.radiologyexams.exceptions.OrthancCommunicationException;
import com.henrique.radiologyexams.exceptions.OrthancIntegrationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
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
        OrthancSystemResponseDTO response;

        try{
            response = restClient.get()
                    .uri("/system")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(OrthancSystemResponseDTO.class);
        } catch (ResourceAccessException ex) {
            throw new OrthancCommunicationException("Could not communicate with orthanc.", ex);
        } catch (RestClientException ex) {
            throw new OrthancIntegrationException("Could not retrieve orthanc system information.", ex);
        }

        if (response == null) {
            throw new OrthancIntegrationException("Orthanc returned an empty system response.");
        }

        return response;
    }

    public OrthancUploadResponseDTO uploadInstance(Resource dicomFile)  {
        OrthancUploadResponseDTO response;

        try {
            response = restClient.post()
                    .uri("/instances")
                    .contentType(MediaType.parseMediaType("application/dicom"))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(dicomFile)
                    .retrieve()
                    .body(OrthancUploadResponseDTO.class);
        } catch (ResourceAccessException ex) {
            throw new OrthancCommunicationException("Could not communicate with orthanc.", ex);
        } catch (RestClientException ex) {
            throw new OrthancIntegrationException("Could not upload the dicom instance to orthanc.", ex);
        }

        if (response == null) {
            throw new OrthancIntegrationException("Orthanc returned an empty upload response.");
        }

        if (!"Success".equals(response.status()) && !"AlreadyStored".equals(response.status())) {
            throw new OrthancIntegrationException("Orthanc did not store the dicom instance.");
        }

        if (response.instanceId() == null || response.instanceId().isBlank()
                || response.parentStudyId() == null || response.parentStudyId().isBlank()) {
            throw new OrthancIntegrationException("Orthanc returned an upload response with missing identifiers.");
        }

        return response;
    }

    public OrthancStudyResponseDTO getStudy(String orthancStudyId) {
        OrthancStudyResponseDTO response;

        try {
            response = restClient.get()
                    .uri("/studies/{id}", orthancStudyId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(OrthancStudyResponseDTO.class);
        } catch (ResourceAccessException ex) {
            throw new OrthancCommunicationException("Could not communicate with orthanc.", ex);
        } catch (RestClientException ex) {
            throw new OrthancIntegrationException("Could not retrieve study information from orthanc.", ex);
        }

        if (response == null) {
            throw new OrthancIntegrationException("Orthanc returned an empty study response.");
        }

        if (response.mainDicomTags() == null || response.mainDicomTags().studyInstanceUid() == null
                || response.mainDicomTags().studyInstanceUid().isBlank()) {
            throw new OrthancIntegrationException("Orthanc returned a study response without a StudyInstanceUID.");
        }

        return response;
    }
}
