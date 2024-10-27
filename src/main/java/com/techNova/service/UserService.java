package com.techNova.service;

import com.techNova.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface UserService {
    void create(User user);
    void update(User user);
    void delete(long id);
    Optional<User> findById(long id);
    List<User> findAll();
    Optional<User> getUserByUsername(String username);
    Optional<User> getUserByEmail(String email);

}
