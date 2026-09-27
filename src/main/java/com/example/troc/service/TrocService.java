package com.example.troc.service;

import com.example.troc.domain.User;
import com.example.troc.repository.UserRepositoryImpl;

public class TrocService {

    // Couplage fort volontaire : TrocService décide lui-même de son implémentation
    // de repository. Impossible de la remplacer sans modifier cette classe.
    private final UserRepositoryImpl userRepository = new UserRepositoryImpl();

    public User inscrireUtilisateur(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        return userRepository.save(user);
    }

    public User consulterUtilisateur(Long id) {
        return userRepository.findById(id);
    }
}
