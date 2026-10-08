package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.dto.UserSummaryResponse;
import com.rtms.backend.entity.Role;
import com.rtms.backend.entity.User;
import com.rtms.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDirectoryControllerTest {

    @Mock
    private UserRepository userRepository;

    private UserDirectoryController controller;

    @BeforeEach
    void setUp() {
        controller = new UserDirectoryController(userRepository);
    }

    @Test
    @DisplayName("getUsers lọc theo role: chỉ trả user active khớp vai trò")
    void testGetUsers_FilterByRole() {
        Role groomRole = new Role();
        groomRole.setName("GROOM");

        Role trainerRole = new Role();
        trainerRole.setName("HEAD_TRAINER");

        User u1 = new User();
        u1.setId(1L);
        u1.setFullName("Groom A");
        u1.setEmail("grooma@rtms.com");
        u1.setRole(groomRole);
        u1.setActive(true);

        User u2 = new User();
        u2.setId(2L);
        u2.setFullName("Trainer B");
        u2.setEmail("trainerb@rtms.com");
        u2.setRole(trainerRole);
        u2.setActive(true);

        User u3 = new User();
        u3.setId(3L);
        u3.setFullName("Groom Inactive");
        u3.setEmail("groom_inactive@rtms.com");
        u3.setRole(groomRole);
        u3.setActive(false);

        when(userRepository.findAll()).thenReturn(List.of(u1, u2, u3));

        ApiResponse<List<UserSummaryResponse>> response = controller.getUsers("GROOM");

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        UserSummaryResponse summary = response.getData().get(0);
        assertEquals(1L, summary.getId());
        assertEquals("Groom A", summary.getFullName());
        assertEquals("GROOM", summary.getRole());
    }

    @Test
    @DisplayName("getUsers không truyền role: trả toàn bộ user active")
    void testGetUsers_NoFilter() {
        Role role = new Role();
        role.setName("GROOM");

        User u1 = new User();
        u1.setId(1L);
        u1.setFullName("User 1");
        u1.setEmail("u1@rtms.com");
        u1.setRole(role);
        u1.setActive(true);

        when(userRepository.findAll()).thenReturn(List.of(u1));

        ApiResponse<List<UserSummaryResponse>> response = controller.getUsers(null);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
    }
}
