package com.travel.security.model;

import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String fullName;
    private final Role role;
    private final Set<String> permissions;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean isDeleted;

    public CustomUserDetails(User user) {
        this(user, Set.of());
    }

    public CustomUserDetails(User user, Collection<String> permissions) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.isDeleted = user.isDeleted();
        this.permissions = permissions != null ? Set.copyOf(permissions) : Set.of();

        List<GrantedAuthority> authList = new ArrayList<>();
        if (user.getRole() != null) {
            authList.add(new SimpleGrantedAuthority(user.getRole().getAuthority()));
        }
        if (permissions != null) {
            for (String perm : permissions) {
                if (perm != null && !perm.isBlank()) {
                    authList.add(new SimpleGrantedAuthority(perm));
                }
            }
        }
        this.authorities = List.copyOf(authList);
    }

    public CustomUserDetails(UUID id, String email, Role role, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = "";
        this.fullName = null;
        this.role = role;
        this.authorities = authorities != null ? List.copyOf(authorities) : List.of();
        this.permissions = authorities != null ? authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_"))
                .collect(Collectors.toUnmodifiableSet()) : Set.of();
        this.isDeleted = false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isDeleted;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !isDeleted;
    }
}
