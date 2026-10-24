package com.henrique.radiologyexams.integrations.orthanc;


import com.henrique.radiologyexams.exceptions.OrthancCommunicationException;
import com.henrique.radiologyexams.exceptions.OrthancIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OrthancClientTest {
    private MockRestServiceServer server;
    private OrthancClient client;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://orthanc.test");

        server = MockRestServiceServer.bindTo(builder).build();
        client = new OrthancClient(builder.build());
    }

    @Test
    void shouldReturnSystemInformation() {
        server.expect(requestTo("http://orthanc.test/system"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                            "Name": "Orthanc",
                            "Version": "1.13.0",
                            "ApiVersion": 31
                        }
                        """, MediaType.APPLICATION_JSON
                ));

        OrthancSystemResponseDTO response = client.getSystem();

        assertEquals("Orthanc", response.name());
        assertEquals("1.13.0", response.version());

        server.verify();
    }

    @Test
    void shouldRejectEmptySystemResponse() {
        server.expect(requestTo("http://orthanc.test/system"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        OrthancIntegrationException ex = assertThrows(OrthancIntegrationException.class, () -> client.getSystem());

        assertEquals("Orthanc returned an empty system response.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldWrapSystemServerError() {
        server.expect(requestTo("http://orthanc.test/system"))
                .andRespond(withServerError());

        OrthancIntegrationException ex = assertThrows(OrthancIntegrationException.class, () -> client.getSystem());

        assertInstanceOf(HttpServerErrorException.class, ex.getCause());

        server.verify();
    }

    @Test
    void shouldWrapSystemCommunicationFailure() {
        server.expect(requestTo("http://orthanc.test/system"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("Connection failed")));

        OrthancCommunicationException ex = assertThrows(OrthancCommunicationException.class, () -> client.getSystem());

        assertInstanceOf(ResourceAccessException.class, ex.getCause());

        server.verify();
    }

    @Test
    void shouldUploadInstanceAndReturnIdentifiers() {
        byte[] dicomBytes = {0, 1, 2, 3};
        ByteArrayResource dicomFile = new ByteArrayResource(dicomBytes);

        server.expect(requestTo("http://orthanc.test/instances"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType("application/dicom"))
                .andExpect(content().bytes(dicomBytes))
                .andRespond(withSuccess("""
                        {
                            "ID": "instance-001",
                            "ParentStudy": "study-001",
                            "Status": "Success",
                            "ParentPatient": "patient-001"
                        }
                        """, MediaType.APPLICATION_JSON)
                );

        OrthancUploadResponseDTO response = client.uploadInstance(dicomFile);

        assertEquals("instance-001", response.instanceId());
        assertEquals("study-001", response.parentStudyId());
        assertEquals("Success", response.status());

        server.verify();
    }

    @Test
    void shouldAcceptAlreadyStoredStatus() {
        byte[] dicomBytes = {0, 1, 2, 3};
        ByteArrayResource dicomFile = new ByteArrayResource(dicomBytes);

        server.expect(requestTo("http://orthanc.test/instances"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType("application/dicom"))
                .andExpect(content().bytes(dicomBytes))
                .andRespond(withSuccess("""
                        {
                            "ID": "instance-001",
                            "ParentStudy": "study-001",
                            "Status": "AlreadyStored",
                            "ParentPatient": "patient-001"
                        }
                        """, MediaType.APPLICATION_JSON)
                );

        OrthancUploadResponseDTO response = client.uploadInstance(dicomFile);

        assertEquals("instance-001", response.instanceId());
        assertEquals("study-001", response.parentStudyId());
        assertEquals("AlreadyStored", response.status());

        server.verify();
    }

    @Test
    void shouldRejectFailureStatus() {
        byte[] dicomBytes = {0, 1, 2, 3};
        ByteArrayResource dicomFile = new ByteArrayResource(dicomBytes);

        server.expect(requestTo("http://orthanc.test/instances"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType("application/dicom"))
                .andExpect(content().bytes(dicomBytes))
                .andRespond(withSuccess("""
                        {
                            "ID": "instance-001",
                            "ParentStudy": "study-001",
                            "Status": "Failure",
                            "ParentPatient": "patient-001"
                        }
                        """, MediaType.APPLICATION_JSON)
                );

        OrthancIntegrationException ex = assertThrows(OrthancIntegrationException.class, () -> client.uploadInstance(dicomFile));

        assertEquals("Orthanc did not store the dicom instance.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldRejectFilteredOutStatus() {
        byte[] dicomBytes = {0, 1, 2, 3};
        ByteArrayResource dicomFile = new ByteArrayResource(dicomBytes);

        server.expect(requestTo("http://orthanc.test/instances"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType("application/dicom"))
                .andExpect(content().bytes(dicomBytes))
                .andRespond(withSuccess("""
                        {
                            "ID": "instance-001",
                            "ParentStudy": "study-001",
                            "Status": "FilteredOut",
                            "ParentPatient": "patient-001"
                        }
                        """, MediaType.APPLICATION_JSON)
                );

        OrthancIntegrationException ex = assertThrows(OrthancIntegrationException.class, () -> client.uploadInstance(dicomFile));

        assertEquals("Orthanc did not store the dicom instance.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldRejectEmptyUploadResponse() {
        byte[] dicomBytes = {0, 1, 2, 3};
        ByteArrayResource dicomFile = new ByteArrayResource(dicomBytes);

        server.expect(requestTo("http://orthanc.test/instances"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType("application/dicom"))
                .andExpect(content().bytes(dicomBytes))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON)
                );

        OrthancIntegrationException ex = assertThrows(OrthancIntegrationException.class, () -> client.uploadInstance(dicomFile));

        assertEquals("Orthanc returned an empty upload response.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldRejectNullInstanceId() {
        byte[] dicomBytes = {0, 1, 2, 3};
        ByteArrayResource dicomFile = new ByteArrayResource(dicomBytes);

        server.expect(requestTo("http://orthanc.test/instances"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType("application/dicom"))
                .andExpect(content().bytes(dicomBytes))
                .andRespond(withSuccess("""
                        {
                            "ID": null,
                            "ParentStudy": "study-001",
                            "Status": "Success",
                            "ParentPatient": "patient-001"
                        }
                        """, MediaType.APPLICATION_JSON)
                );

        OrthancIntegrationException ex = assertThrows(OrthancIntegrationException.class, () -> client.uploadInstance(dicomFile));

        assertEquals("Orthanc returned an upload response with missing identifiers.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldRejectBlankInstanceId() {
        byte[] dicomBytes = {0, 1, 2, 3};
        ByteArrayResource dicomFile = new ByteArrayResource(dicomBytes);

        server.expect(requestTo("http://orthanc.test/instances"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType("application/dicom"))
                .andExpect(content().bytes(dicomBytes))
                .andRespond(withSuccess("""
                        {
                            "ID": " ",
                            "ParentStudy": "study-001",
                            "Status": "Success",
                            "ParentPatient": "patient-001"
                        }
                        """, MediaType.APPLICATION_JSON)
                );

        OrthancIntegrationException ex = assertThrows(OrthancIntegrationException.class, () -> client.uploadInstance(dicomFile));

        assertEquals("Orthanc returned an upload response with missing identifiers.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldRejectNullParentStudyId() {
        byte[] dicomBytes = {0, 1, 2, 3};
        ByteArrayResource dicomFile = new ByteArrayResource(dicomBytes);

        server.expect(requestTo("http://orthanc.test/instances"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType("application/dicom"))
                .andExpect(content().bytes(dicomBytes))
                .andRespond(withSuccess("""
                        {
                            "ID": "instance-001",
                            "ParentStudy": null,
                            "Status": "Success",
                            "ParentPatient": "patient-001"
                        }
                        """, MediaType.APPLICATION_JSON)
                );

        OrthancIntegrationException ex = assertThrows(OrthancIntegrationException.class, () -> client.uploadInstance(dicomFile));

        assertEquals("Orthanc returned an upload response with missing identifiers.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldRejectBlankParentStudyId() {
        byte[] dicomBytes = {0, 1, 2, 3};
        ByteArrayResource dicomFile = new ByteArrayResource(dicomBytes);

        server.expect(requestTo("http://orthanc.test/instances"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType("application/dicom"))
                .andExpect(content().bytes(dicomBytes))
                .andRespond(withSuccess("""
                        {
                            "ID": "instance-001",
                            "ParentStudy": " ",
                            "Status": "Success",
                            "ParentPatient": "patient-001"
                        }
                        """, MediaType.APPLICATION_JSON)
                );

        OrthancIntegrationException ex = assertThrows(OrthancIntegrationException.class, () -> client.uploadInstance(dicomFile));

        assertEquals("Orthanc returned an upload response with missing identifiers.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldWrapUploadServerError() {
        byte[] dicomBytes = {0, 1, 2, 3};
        ByteArrayResource dicomFile = new ByteArrayResource(dicomBytes);

        server.expect(requestTo("http://orthanc.test/instances"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        OrthancIntegrationException ex = assertThrows(
                OrthancIntegrationException.class, () -> client.uploadInstance(dicomFile)
        );

        assertInstanceOf(HttpServerErrorException.class, ex.getCause());

        server.verify();
    }

    @Test
    void shouldWrapUploadUnauthorizedError() {
        byte[] dicomBytes = {0, 1, 2, 3};
        ByteArrayResource dicomFile = new ByteArrayResource(dicomBytes);

        server.expect(requestTo("http://orthanc.test/instances"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        OrthancIntegrationException ex = assertThrows(
                OrthancIntegrationException.class, () -> client.uploadInstance(dicomFile)
        );

        assertInstanceOf(HttpClientErrorException.class, ex.getCause());

        server.verify();
    }

    @Test
    void shouldWrapUploadCommunicationFailure() {
        byte[] dicomBytes = {0, 1, 2, 3};
        ByteArrayResource dicomFile = new ByteArrayResource(dicomBytes);

        server.expect(requestTo("http://orthanc.test/instances"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(new IOException("Connection failed")));

        OrthancCommunicationException ex = assertThrows(
                OrthancCommunicationException.class, () -> client.uploadInstance(dicomFile)
        );

        assertInstanceOf(ResourceAccessException.class, ex.getCause());

        server.verify();
    }

    @Test
    void shouldReturnStudyInformation() {
        server.expect(requestTo("http://orthanc.test/studies/study-001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                            "ID": "study-001",
                            "MainDicomTags": {"StudyInstanceUID": "1.2.840.123", "StudyDescription": "breast ct"}
                        }
                        """, MediaType.APPLICATION_JSON
                ));

        OrthancStudyResponseDTO response = client.getStudy("study-001");

        assertEquals("1.2.840.123", response.mainDicomTags().studyInstanceUid());

        server.verify();
    }

    @Test
    void shouldRejectEmptyStudyResponse() {
        server.expect(requestTo("http://orthanc.test/studies/study-001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        OrthancIntegrationException ex = assertThrows(
                OrthancIntegrationException.class,
                () -> client.getStudy("study-001")
        );

        assertEquals("Orthanc returned an empty study response.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldRejectMissingStudyInstanceUID() {
        server.expect(requestTo("http://orthanc.test/studies/study-001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                            "MainDicomTags": {}
                        }
                        """, MediaType.APPLICATION_JSON));

        OrthancIntegrationException ex = assertThrows(
                OrthancIntegrationException.class,
                () -> client.getStudy("study-001")
        );

        assertEquals("Orthanc returned a study response without a StudyInstanceUID.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldRejectMissingMainDicomTags() {
        server.expect(requestTo("http://orthanc.test/studies/study-001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                            
                        }
                        """, MediaType.APPLICATION_JSON));

        OrthancIntegrationException ex = assertThrows(
                OrthancIntegrationException.class,
                () -> client.getStudy("study-001")
        );

        assertEquals("Orthanc returned a study response without a StudyInstanceUID.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldRejectBlankStudyInstanceUID() {
        server.expect(requestTo("http://orthanc.test/studies/study-001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                            "MainDicomTags": {"StudyInstanceUID": " "}
                        }
                        """, MediaType.APPLICATION_JSON));

        OrthancIntegrationException ex = assertThrows(
                OrthancIntegrationException.class,
                () -> client.getStudy("study-001")
        );

        assertEquals("Orthanc returned a study response without a StudyInstanceUID.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldWrapStudyServerError() {
        server.expect(requestTo("http://orthanc.test/studies/study-001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        OrthancIntegrationException ex = assertThrows(OrthancIntegrationException.class, () -> client.getStudy("study-001"));

        assertInstanceOf(HttpServerErrorException.class, ex.getCause());

        server.verify();
    }

    @Test
    void shouldWrapStudyNotFoundError() {
        server.expect(requestTo("http://orthanc.test/studies/study-001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        OrthancIntegrationException ex = assertThrows(
                OrthancIntegrationException.class, () -> client.getStudy("study-001"));

        assertInstanceOf(HttpClientErrorException.class, ex.getCause());

        server.verify();
    }

    @Test
    void shouldWrapStudyCommunicationFailure() {
        server.expect(requestTo("http://orthanc.test/studies/study-001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("Connection failed")));

        OrthancCommunicationException ex = assertThrows(
                OrthancCommunicationException.class, () -> client.getStudy("study-001"));

        assertInstanceOf(ResourceAccessException.class, ex.getCause());

        server.verify();
    }

    @Test
    void shouldWrapMalformedStudyResponse() {
        server.expect(requestTo("http://orthanc.test/studies/study-001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                            "MainDicomTags":
                        }
                        """, MediaType.APPLICATION_JSON));

        OrthancIntegrationException ex = assertThrows(
                OrthancIntegrationException.class,
                () -> client.getStudy("study-001")
        );

        assertInstanceOf(RestClientException.class, ex.getCause());

        server.verify();
    }
}
