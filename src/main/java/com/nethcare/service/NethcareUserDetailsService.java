package com.nethcare.service;

import com.nethcare.model.User;
import com.nethcare.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security reads users from the users table through this.
 *
 * Role comes back as ROLE_ADMIN, ROLE_OPTICIAN etc so the config can use
 * hasRole("ADMIN"). An INACTIVE user is returned as locked, so login fails.
 */
@Service
public class NethcareUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public NethcareUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user named " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(toAuthorities(user))
                .accountLocked(!user.isActive())
                .build();
    }

    private Collection<? extends GrantedAuthority> toAuthorities(User user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
}
