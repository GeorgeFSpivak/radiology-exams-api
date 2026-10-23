package com.henrique.radiologyexams.integrations.orthanc;


import com.henrique.radiologyexams.config.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Import({PostgresTestConfiguration.class})
class OrthancClientIT {
    @Autowired
    private OrthancClient client;

    @Test
    void shouldGetSystemFormationFromOrthanc() {
        OrthancSystemResponseDTO response = client.getSystem();

        assertNotNull(response.name());
        assertFalse(response.name().isBlank());

        assertNotNull(response.version());
        assertFalse(response.version().isBlank());
    }
}
