package com.vogulev.regreso.security;

import com.vogulev.regreso.entity.Practitioner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class PractitionerDetails implements UserDetails {

    private final Practitioner practitioner;

    public UUID getId() {
        return practitioner.getId();
    }

    public Practitioner getPractitioner() {
        return practitioner;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return practitioner.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return practitioner.getEmail();
    }
}
