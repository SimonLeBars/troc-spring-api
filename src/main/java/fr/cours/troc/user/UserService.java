package fr.cours.troc.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public List<User> findAll() {
        return users.findAll();
    }

    public User findById(Long id) {
        return users.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur " + id + " introuvable"));
    }

    public User create(User user) {
        user.setId(null); // l'id est attribué par le repository, pas par le client
        return users.save(user);
    }

    public User update(Long id, User user) {
        findById(id); // 404 si l'utilisateur n'existe pas
        user.setId(id);
        return users.save(user);
    }

    public void delete(Long id) {
        findById(id);
        users.deleteById(id);
    }
}
