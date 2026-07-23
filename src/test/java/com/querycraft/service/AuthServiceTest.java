package com.querycraft.service;

import com.querycraft.domain.Role;
import com.querycraft.domain.UserEntity;
import com.querycraft.domain.dto.AuthResponse;
import com.querycraft.domain.dto.LoginRequest;
import com.querycraft.domain.dto.RegisterRequest;
import com.querycraft.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

class AuthServiceTest {

    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository);
    }

    @Test
    @DisplayName("Should register user account successfully")
    void testRegisterUser() {
        given(userRepository.existsByUsername(anyString())).willReturn(false);
        given(userRepository.existsByEmail(anyString())).willReturn(false);

        RegisterRequest req = RegisterRequest.builder()
                .username("newdev")
                .email("newdev@company.com")
                .password("securePass123")
                .role(Role.ROLE_USER)
                .build();

        AuthResponse regResp = authService.register(req);
        assertNotNull(regResp);
        assertEquals("newdev", regResp.getUsername());
        assertEquals(Role.ROLE_USER, regResp.getRole());
        assertTrue(regResp.getToken().startsWith("qc_token_"));
    }
}
