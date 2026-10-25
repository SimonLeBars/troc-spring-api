package fr.cours.troc.user;

import org.springframework.stereotype.Service;

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

    public User create(User user) {
        return users.save(user);
    }
}
