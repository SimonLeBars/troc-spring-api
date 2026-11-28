package fr.cours.troc.security;

import fr.cours.troc.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Où Spring Security trouve les comptes. Appelé uniquement à la connexion (POST /auth/token).
@Service
public class AccountDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AccountDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsername(username)
                // le User de Spring Security, à ne pas confondre avec notre entité
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .roles("USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
