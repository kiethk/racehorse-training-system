package com.rtms.backend.admission.controller;

import com.rtms.backend.admission.dto.AdmissionDetailResponse;
import com.rtms.backend.admission.dto.GroomAssignmentHistoryItem;
import com.rtms.backend.admission.enums.AssignmentEndReason;
import com.rtms.backend.admission.service.AdmissionQueryService;
import com.rtms.backend.admission.service.GroomAssignmentService;
import com.rtms.backend.admission.service.GroomWorkloadService;
import com.rtms.backend.config.GlobalExceptionHandler;
import com.rtms.backend.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AdmissionGroomAssignmentControllerTest {
    AnnotationConfigApplicationContext context;
    GroomAssignmentService assignments;
    GroomWorkloadService workload;
    AdmissionQueryService query;
    MockMvc mvc;

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean GroomAssignmentService assignments() { return mock(GroomAssignmentService.class); }
        @Bean GroomWorkloadService workload() { return mock(GroomWorkloadService.class); }
        @Bean AdmissionQueryService query() { return mock(AdmissionQueryService.class); }
        @Bean AdmissionGroomAssignmentController controller(GroomAssignmentService assignments,
                GroomWorkloadService workload, AdmissionQueryService query) {
            return new AdmissionGroomAssignmentController(assignments, workload, query);
        }
    }

    @BeforeEach
    void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        assignments = context.getBean(GroomAssignmentService.class);
        workload = context.getBean(GroomWorkloadService.class);
        query = context.getBean(AdmissionQueryService.class);
        mvc = standaloneSetup(context.getBean(AdmissionGroomAssignmentController.class))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        authenticate(9L, "CLUB_MANAGER", "ADMISSION_APPLICATION_MANAGER_REVIEW");
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    void authenticate(Long id, String role, String permission) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(id, "user@test.invalid", role), "",
                List.of(new SimpleGrantedAuthority(permission))));
    }

    @Test
    void managerReassignReturnsDetailWithExactContract() throws Exception {
        AdmissionDetailResponse detail = new AdmissionDetailResponse(); detail.setAdmissionId(10L);
        detail.setAssignedGroomId(2L); detail.setAssignedGroomName("Groom Two");
        detail.setGroomAssignedAt(LocalDateTime.of(2026, 11, 6, 8, 0));
        detail.setGroomReviewDueAt(detail.getGroomAssignedAt().plusHours(72));
        detail.setAssignedGroomFull(true);
        when(query.getAdmissionDetail(10L)).thenReturn(detail);
        mvc.perform(post("/api/admissions/10/groom-assignment").contentType(MediaType.APPLICATION_JSON)
                .content("{\"groomId\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.assignedGroomId").value(2))
                .andExpect(jsonPath("$.data.assignedGroomName").value("Groom Two"))
                .andExpect(jsonPath("$.data.groomAssignedAt").exists())
                .andExpect(jsonPath("$.data.groomReviewDueAt").exists())
                .andExpect(jsonPath("$.data.assignedGroomFull").value(true));
        verify(assignments).reassign(10L, AssignmentEndReason.MANUAL, 9L, 2L);
    }

    @Test
    void invalidBodyIsRejectedBeforeReassignment() throws Exception {
        for (String body : List.of("{}", "{\"groomId\":0}", "{\"groomId\":-1}")) {
            mvc.perform(post("/api/admissions/10/groom-assignment").contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(assignments, query);
    }

    @Test
    void groomCannotReassignOrReadManagerPicker() throws Exception {
        authenticate(2L, "GROOM", "ADMISSION_APPLICATION_VIEW");
        mvc.perform(post("/api/admissions/10/groom-assignment").contentType(MediaType.APPLICATION_JSON)
                .content("{\"groomId\":2}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admissions/quarantine-grooms")).andExpect(status().isForbidden());
        verifyNoInteractions(assignments, workload, query);
    }

    @Test
    void managerPickerUsesGroomIdFieldAndIncludesFullGrooms() throws Exception {
        when(workload.describeQuarantineGrooms()).thenReturn(List.of(
                new GroomWorkloadService.GroomCandidate(2L, "Groom Two", 3L, true)));
        mvc.perform(get("/api/admissions/quarantine-grooms")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groomId").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Groom Two"))
                .andExpect(jsonPath("$.data[0].workload").value(3))
                .andExpect(jsonPath("$.data[0].full").value(true));
    }

    @Test
    void assignedGroomHistoryDelegatesScopeCheckAndUsesContract() throws Exception {
        authenticate(2L, "GROOM", "ADMISSION_APPLICATION_VIEW");
        when(assignments.history(eq(10L), any())).thenReturn(List.of(new GroomAssignmentHistoryItem(
                100L, 2L, "Groom Two", LocalDateTime.of(2026, 11, 6, 8, 0), null, null, "Manager")));
        mvc.perform(get("/api/admissions/10/groom-assignments")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(100))
                .andExpect(jsonPath("$.data[0].groomName").value("Groom Two"))
                .andExpect(jsonPath("$.data[0].assignedByName").value("Manager"))
                .andExpect(jsonPath("$.data[0].assignedAt").exists());
        verify(assignments).history(eq(10L), argThat(user -> user.getUserId().equals(2L)));
    }
}
