package com.rtms.backend.identity.controller;

import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.Matchers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@EnabledIfEnvironmentVariable(named="RTMS_PROFILE_TEST_DB_URL",matches=".+")
@SpringBootTest(properties={"spring.config.import=optional:classpath:/no-cloud-profile-tests.properties",
        "spring.datasource.username=rtms_user","spring.datasource.password=rtms_password",
        "rtms.storage.cloudinary-url=","rtms.storage.upload-dir=./target/profile-test-files","spring.jpa.show-sql=false"})
@Transactional
class MyProfilePostgresTest {
    @DynamicPropertySource static void isolated(DynamicPropertyRegistry properties) {
        String url=System.getenv("RTMS_PROFILE_TEST_DB_URL");
        if(!url.matches("jdbc:postgresql://localhost:5433/rtms_profile_test_[0-9]+")) throw new IllegalStateException("Disposable local profile DB required");
        properties.add("spring.datasource.url",()->url);
    }
    @Autowired WebApplicationContext web;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;
    @BeforeEach void setup() { mvc=webAppContextSetup(web).apply(springSecurity()).build(); }

    @ParameterizedTest @CsvSource({"1,CLUB_MANAGER","5,HORSE_OWNER","8,HEAD_TRAINER","6,VETERINARIAN","10,GROOM"})
    void eachRoleCanEditOnlyTheirOwnBasicProfileAndUploadPhoto(long id,String role) throws Exception {
        var image=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(12,12,BufferedImage.TYPE_INT_RGB),"png",image);
        String otherName=jdbc.queryForObject("SELECT full_name FROM users WHERE id=?",String.class,id==1?5:1);
        var actor=new UsernamePasswordAuthenticationToken(new AuthenticatedUser(id,"demo@test.invalid",role),null,List.of());
        mvc.perform(multipart("/api/auth/me/profile").file(profile("{\"fullName\":\" Updated Name \",\"phone\":\"0909123456\",\"address\":\"New address\",\"removeAvatar\":false}"))
                        .file(new MockMultipartFile("avatar","avatar.png","image/png",image.toByteArray())).param("userId",id==1?"5":"1").with(authentication(actor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value(id))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name")).andExpect(jsonPath("$.data.phone").value("0909123456"))
                .andExpect(jsonPath("$.data.role").value(role)).andExpect(jsonPath("$.data.avatarUrl").value(Matchers.startsWith("/api/auth/me/avatar?v=")))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        mvc.perform(get("/api/auth/me/avatar").with(authentication(actor))).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(get("/api/auth/me").with(authentication(actor))).andExpect(status().isOk()).andExpect(jsonPath("$.data.address").value("New address"));
        assertEquals(otherName,jdbc.queryForObject("SELECT full_name FROM users WHERE id=?",String.class,id==1?5:1));
    }

    @ParameterizedTest @ValueSource(strings={"role","email","userId","isActive","password","avatarUrl","profile"})
    void protectedFieldsCannotBeMassAssigned(String field) throws Exception {
        var actor=new UsernamePasswordAuthenticationToken(new AuthenticatedUser(5L,"owner@rtms.com","HORSE_OWNER"),null,List.of());
        mvc.perform(multipart("/api/auth/me/profile").file(profile("{\"fullName\":\"Changed\",\""+field+"\":\"injected\"}")).with(authentication(actor)))
                .andExpect(status().isBadRequest());
        assertEquals("Test Owner",jdbc.queryForObject("SELECT full_name FROM users WHERE id=5",String.class));
    }

    @Test void anonymousRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/auth/me/avatar")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/auth/me/profile").file(profile("{\"fullName\":\"Name\"}"))).andExpect(status().isUnauthorized());
    }
    @Test void missingAvatarIsNotFound() throws Exception {
        var actor=new UsernamePasswordAuthenticationToken(new AuthenticatedUser(5L,"owner@rtms.com","HORSE_OWNER"),null,List.of());
        mvc.perform(get("/api/auth/me/avatar").with(authentication(actor))).andExpect(status().isNotFound());
    }
    @ParameterizedTest @ValueSource(strings={"", "   "})
    void blankNameDoesNotChangeProfile(String name) throws Exception {
        var actor=new UsernamePasswordAuthenticationToken(new AuthenticatedUser(5L,"owner@rtms.com","HORSE_OWNER"),null,List.of());
        mvc.perform(multipart("/api/auth/me/profile").file(profile("{\"fullName\":\""+name+"\"}")).with(authentication(actor)))
                .andExpect(status().isBadRequest());
        assertEquals("Test Owner",jdbc.queryForObject("SELECT full_name FROM users WHERE id=5",String.class));
    }
    @Test void twentyCharacterPhoneCanBeSaved() throws Exception {
        var actor=new UsernamePasswordAuthenticationToken(new AuthenticatedUser(5L,"owner@rtms.com","HORSE_OWNER"),null,List.of());
        String phone="12345678901234567890";
        mvc.perform(multipart("/api/auth/me/profile").file(profile("{\"fullName\":\"Updated Name\",\"phone\":\""+phone+"\",\"removeAvatar\":false}")).with(authentication(actor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.phone").value(phone));
        assertEquals(phone,jdbc.queryForObject("SELECT phone FROM users WHERE id=5",String.class));
    }

    @ParameterizedTest @ValueSource(ints={21,30})
    void oversizedPhoneIsRejectedWithoutChangingProfile(int length) throws Exception {
        var actor=new UsernamePasswordAuthenticationToken(new AuthenticatedUser(5L,"owner@rtms.com","HORSE_OWNER"),null,List.of());
        String originalPhone=jdbc.queryForObject("SELECT phone FROM users WHERE id=5",String.class);
        mvc.perform(multipart("/api/auth/me/profile").file(profile("{\"fullName\":\"Changed\",\"phone\":\""+"1".repeat(length)+"\",\"removeAvatar\":false}")).with(authentication(actor)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Phone number must be at most 20 characters"));
        assertEquals("Test Owner",jdbc.queryForObject("SELECT full_name FROM users WHERE id=5",String.class));
        assertEquals(originalPhone,jdbc.queryForObject("SELECT phone FROM users WHERE id=5",String.class));
    }

    private static MockMultipartFile profile(String body) { return new MockMultipartFile("profile","","application/json",body.getBytes(StandardCharsets.UTF_8)); }
}
