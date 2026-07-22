package com.medibridge.common.security;

import com.medibridge.common.enums.UserType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The authenticated principal, rebuilt from JWT claims on every request.
 *
 * <p>{@code id} is a String because doctors use a UUID PK while patients and
 * admins use ints. Callers that need the numeric form use {@link #idAsInt()}.
 */
@Getter
@RequiredArgsConstructor
public class SecurityUser implements UserDetails {

    private final String id;
    private final String email;
    private final String fullName;
    private final UserType userType;

    public Integer idAsInt() {
        return Integer.valueOf(id);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(userType.authority()));
    }

    /** Never available from a JWT - the token proves authentication already. */
    @Override
    public String getPassword() {
        return null;
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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
