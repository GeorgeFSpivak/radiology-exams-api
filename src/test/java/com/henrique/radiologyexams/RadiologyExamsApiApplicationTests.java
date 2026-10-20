package com.henrique.radiologyexams;

import com.henrique.radiologyexams.config.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;


@SpringBootTest
@Import(PostgresTestConfiguration.class)
class RadiologyExamsApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
