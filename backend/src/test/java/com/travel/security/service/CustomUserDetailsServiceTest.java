package com.travel.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travel.security.model.CustomUserDetails;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import com.travel.user.repository.RolePermissionRepository;
import com.travel.user.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername should return CustomUserDetails with combined role and permissions")
    void loadUserByUsername_success() {
        String email = "test@gmail.com";
        User mockUser = User.builder()
            .fullName("Test User")
            .email(email)
            .passwordHash("hashed_secret")
            .role(Role.USER)
            .build();

        Set<String> mockPermissions = Set.of("place:read", "trip:read");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(rolePermissionRepository.findPermissionNamesByRole(Role.USER)).thenReturn(mockPermissions);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("hashed_secret");

        Set<String> authorityNames = userDetails
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(java.util.stream.Collectors.toSet());

        assertThat(authorityNames).containsExactlyInAnyOrder("ROLE_USER", "place:read", "trip:read");

        verify(userRepository).findByEmail(email);
        verify(rolePermissionRepository).findPermissionNamesByRole(Role.USER);
    }

    @Test
    @DisplayName("loadUserByUsername should throw UsernameNotFoundException when user does not exist")
    void loadUserByUsername_notFound() {
        String email = "nonexistent@gmail.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found with email: " + email);
    }
}
