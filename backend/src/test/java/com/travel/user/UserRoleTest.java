package com.travel.user;

import com.travel.security.model.CustomUserDetails;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UserRoleTest {

    @Test
    @DisplayName("Role.USER should have authority ROLE_USER")
    void roleUser_shouldHaveCorrectAuthority() {
        assertThat(Role.USER.getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Role.ADMIN should have authority ROLE_ADMIN")
    void roleAdmin_shouldHaveCorrectAuthority() {
        assertThat(Role.ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("CustomUserDetails should correctly map User entity to Spring Security UserDetails")
    void customUserDetails_shouldMapUserCorrectly() {
        User user = User.builder()
                .fullName("Nguyen Van A")
                .email("vana@gmail.com")
                .passwordHash("hashed_password_123")
                .role(Role.ADMIN)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertThat(userDetails.getUsername()).isEqualTo("vana@gmail.com");
        assertThat(userDetails.getPassword()).isEqualTo("hashed_password_123");
        assertThat(userDetails.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(userDetails.getRole()).isEqualTo(Role.ADMIN);
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("CustomUserDetails with soft-deleted user should be disabled")
    void customUserDetails_withSoftDeletedUser_shouldBeDisabled() {
        User user = User.builder()
                .fullName("Nguyen Van B")
                .email("vanb@gmail.com")
                .passwordHash("hashed_password_123")
                .role(Role.USER)
                .build();

        user.softDelete();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }
}
