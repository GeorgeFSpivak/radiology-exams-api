package com.henrique.radiologyexams.integrations.orthanc;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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

        RestClientException ex = assertThrows(RestClientException.class, () -> client.getSystem());

        assertEquals("Orthanc returned an empty system response.", ex.getMessage());

        server.verify();
    }

    @Test
    void shouldPropagateServerError() {
        server.expect(requestTo("http://orthanc.test/system"))
                .andRespond(withServerError());

        assertThrows(HttpServerErrorException.class, () -> client.getSystem());

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

        RestClientException ex = assertThrows(RestClientException.class, () -> client.uploadInstance(dicomFile));

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

        RestClientException ex = assertThrows(RestClientException.class, () -> client.uploadInstance(dicomFile));

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

        RestClientException ex = assertThrows(RestClientException.class, () -> client.uploadInstance(dicomFile));

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

        RestClientException ex = assertThrows(RestClientException.class, () -> client.uploadInstance(dicomFile));

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

        RestClientException ex = assertThrows(RestClientException.class, () -> client.uploadInstance(dicomFile));

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

        RestClientException ex = assertThrows(RestClientException.class, () -> client.uploadInstance(dicomFile));

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

        RestClientException ex = assertThrows(RestClientException.class, () -> client.uploadInstance(dicomFile));

        assertEquals("Orthanc returned an upload response with missing identifiers.", ex.getMessage());

        server.verify();
    }
}
