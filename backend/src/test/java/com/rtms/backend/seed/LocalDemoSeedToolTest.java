package com.rtms.backend.seed;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Manual data tool, NOT an ordinary test. Commits local demo data only when explicitly enabled. */
@EnabledIfSystemProperty(named = "rtms.seed.local", matches = "true")
@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.config.import=optional:classpath:/no-cloud-seed.properties",
        "spring.datasource.url=jdbc:postgresql://localhost:5433/rtms_db",
        "spring.datasource.username=rtms_user", "spring.datasource.password=rtms_password",
        "rtms.storage.cloudinary-url=", "rtms.storage.upload-dir=./uploads/admissions",
        "spring.jpa.show-sql=false", "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.springframework.boot.security.autoconfigure=ERROR",
        "logging.level.com.rtms.backend=ERROR"
})
class LocalDemoSeedToolTest extends DemoSeedWorkflow {
    @DynamicPropertySource
    static void localDatabaseOnly(DynamicPropertyRegistry properties) {
        String database=System.getenv().getOrDefault("RTMS_DEMO_SEED_DATABASE","rtms_db");
        if (!database.matches("rtms_db|rtms_seed_rehearsal_[0-9]+")) throw new IllegalArgumentException("Local seed database name rejected");
        properties.add("spring.datasource.url",()->"jdbc:postgresql://localhost:5433/"+database);
        properties.add("spring.datasource.username",()->"rtms_user");
        properties.add("spring.datasource.password",()->"rtms_password");
        properties.add("rtms.storage.cloudinary-url",()->"");
        properties.add("rtms.storage.upload-dir",()->database.equals("rtms_db")?"./uploads/admissions":"./target/demo-seed/rehearsal-files");
    }
    @Override
    protected void verifyTarget(String jdbcUrl) {
        assertEquals("jdbc:postgresql://localhost:5433/"+System.getenv().getOrDefault("RTMS_DEMO_SEED_DATABASE","rtms_db"),jdbcUrl,
                "LOCAL ONLY: refusing another database");
    }
}
