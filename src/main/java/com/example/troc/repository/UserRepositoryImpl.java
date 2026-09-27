package com.example.troc.repository;

import com.example.troc.domain.User;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class UserRepositoryImpl {

    private final Map<Long, User> users = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(nextId.getAndIncrement());
        }
        users.put(user.getId(), user);
        return user;
    }

    public User findById(Long id) {
        return users.get(id);
    }
}
