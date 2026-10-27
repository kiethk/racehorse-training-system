package com.rtms.backend.seed;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Explicit cloud operation only. Not executed by normal tests or application startup. */
@EnabledIfSystemProperty(named="rtms.seed.cloud",matches="true")
@SpringBootTest(properties={
        "spring.profiles.active=cloud", "spring.config.import=optional:classpath:/no-cloud-seed.properties",
        "spring.flyway.enabled=false", "spring.jpa.show-sql=false", "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.orm.connections.pooling=ERROR",
        "logging.level.com.rtms.backend=ERROR", "logging.level.org.springframework.boot.security.autoconfigure=ERROR",
        "spring.datasource.hikari.maximum-pool-size=3", "spring.datasource.hikari.minimum-idle=1"
})
class CloudDemoSeedToolTest extends DemoSeedWorkflow {
    @DynamicPropertySource
    static void reviewedCloudOnly(DynamicPropertyRegistry properties) {
        String url=Objects.requireNonNull(System.getenv("RTMS_SEED_CLOUD_TARGET"),"Run the reviewed cloud seed script");
        if (!url.equals(System.getenv("DB_URL"))) throw new IllegalArgumentException("Cloud target mismatch");
        URI uri=URI.create(url.replaceFirst("^jdbc:",""));
        if (!"postgresql".equals(uri.getScheme()) || uri.getHost()==null || !uri.getHost().endsWith(".neon.tech")
                || uri.getQuery()==null || !uri.getQuery().contains("sslmode=require"))
            throw new IllegalArgumentException("Only the reviewed SSL Neon target is allowed");
        String cloud=Objects.requireNonNull(System.getenv("CLOUDINARY_URL"),"Cloudinary is required");
        if (!cloud.startsWith("cloudinary://") || cloud.isBlank()) throw new IllegalArgumentException("Cloudinary must be configured");
        properties.add("spring.datasource.url",()->url);
        properties.add("spring.datasource.username",()->Objects.requireNonNull(System.getenv("DB_USERNAME")));
        properties.add("spring.datasource.password",()->Objects.requireNonNull(System.getenv("DB_PASSWORD")));
        properties.add("rtms.storage.cloudinary-url",()->cloud);
    }

    @Override protected boolean cloudMode() { return true; }

    @Override protected void verifyTarget(String jdbcUrl) throws Exception {
        assertTrue(Objects.equals(System.getenv("RTMS_SEED_CLOUD_TARGET"),jdbcUrl),"Cloud target mismatch");
        assertFalse(Boolean.getBoolean("rtms.seed.addOwners"),"Local supplement is forbidden on cloud");
        Path backup=Path.of(Objects.requireNonNull(System.getenv("RTMS_SEED_BACKUP"))).toRealPath();
        assertTrue(backup.startsWith(Path.of("target/cloud-seed/backups").toRealPath()),"Unexpected backup location");
        byte[] bytes=Files.readAllBytes(backup);
        assertTrue(bytes.length>1000 && new String(bytes,0,5,StandardCharsets.US_ASCII).equals("PGDMP"));
        assertEquals(System.getenv("RTMS_SEED_BACKUP_SHA256"),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),"Backup checksum mismatch");
        assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='202610171604' AND success=true",Long.class));
    }
}
