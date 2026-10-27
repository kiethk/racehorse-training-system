package com.rtms.backend.horse.controller;

import com.rtms.backend.admission.controller.OwnerAdmissionController;
import com.rtms.backend.admission.dto.CreateOwnerAdmissionRequest;
import com.rtms.backend.admission.service.AdmissionUelnPolicy;
import com.rtms.backend.admission.service.OwnerAdmissionService;
import com.rtms.backend.config.GlobalExceptionHandler;
import com.rtms.backend.horse.dto.CreateHorseRequest;
import com.rtms.backend.horse.enums.HorseSex;
import com.rtms.backend.horse.service.HorseService;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/** HTTP deserialization/validation only; authorization is covered by the real-context tests. */
class HorseSexRequestTest {
    private OwnerAdmissionService admissions;
    private HorseService horses;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        admissions = mock(OwnerAdmissionService.class);
        horses = mock(HorseService.class);
        mvc = standaloneSetup(
                new OwnerAdmissionController(admissions, mock(AdmissionUelnPolicy.class)),
                new HorseController(horses, null, null, null))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == AuthenticatedUser.class;
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                            NativeWebRequest request, WebDataBinderFactory binderFactory) {
                        return new AuthenticatedUser(7L, "owner@test.invalid", "HORSE_OWNER");
                    }
                })
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"sex\":null", ",\"sex\":\"GELDING\"", ",\"sex\":\"UNKNOWN\"",
            ",\"sex\":\"\"", ",\"sex\":0", ",\"sex\":1", ",\"sex\":true"})
    void invalidSexCannotReachEitherCreationService(String sexField) throws Exception {
        String body = "{\"name\":\"Pegasus\"" + sexField + "}";
        mvc.perform(multipart("/api/owner/admissions")
                        .file(jsonPart("candidate", body))
                        .file(jsonPart("documents", "[]"))
                        .file(new MockMultipartFile("files", "photo.jpg", "image/jpeg", new byte[]{1})))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/horses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(admissions, horses);
    }

    @ParameterizedTest
    @EnumSource(HorseSex.class)
    void bothSupportedValuesReachBothCreationServices(HorseSex sex) throws Exception {
        String body = "{\"name\":\"Pegasus\",\"sex\":\"" + sex.name() + "\"}";
        mvc.perform(multipart("/api/owner/admissions")
                        .file(jsonPart("candidate", body))
                        .file(jsonPart("documents", "[]"))
                        .file(new MockMultipartFile("files", "photo.jpg", "image/jpeg", new byte[]{1})))
                .andExpect(status().isCreated());
        var admission = ArgumentCaptor.forClass(CreateOwnerAdmissionRequest.class);
        verify(admissions).submit(eq(7L), admission.capture(), anyList(), anyList());
        assertEquals(sex, admission.getValue().sex());

        mvc.perform(post("/api/horses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        var horse = ArgumentCaptor.forClass(CreateHorseRequest.class);
        verify(horses).createHorse(horse.capture());
        assertEquals(sex, horse.getValue().getSex());
    }

    private MockMultipartFile jsonPart(String name, String body) {
        return new MockMultipartFile(name, "", "application/json", body.getBytes(StandardCharsets.UTF_8));
    }
}
