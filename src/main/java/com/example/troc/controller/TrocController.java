package com.example.troc.controller;

import com.example.troc.domain.User;
import com.example.troc.service.TrocService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class TrocController {

    // Couplage fort volontaire, comme pour TrocService : le contrôleur décide
    // lui-même de son implémentation de service. On y reviendra à l'étape suivante.
    private final TrocService trocService = new TrocService();

    @PostMapping
    public User inscrire(@RequestBody User user) {
        return trocService.inscrireUtilisateur(user.getUsername(), user.getEmail());
    }

    @GetMapping("/{id}")
    public User consulter(@PathVariable Long id) {
        return trocService.consulterUtilisateur(id);
    }
}
