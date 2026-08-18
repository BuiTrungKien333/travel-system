package com.travel.security.model;

import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

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
        this(user, Collections.emptySet());
    }

    public CustomUserDetails(User user, Collection<String> permissions) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.isDeleted = user.isDeleted();
        this.permissions = permissions != null ? Set.copyOf(permissions) : Collections.emptySet();

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
        this.authorities = Collections.unmodifiableList(authList);
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
