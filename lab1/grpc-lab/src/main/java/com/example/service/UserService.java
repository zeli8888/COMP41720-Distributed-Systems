package com.example.service;

import com.example.model.User;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(String id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User createUser(User user) {
        user.setId(null);
        if (user.getName() == null || user.getEmail() == null) {
            throw new IllegalArgumentException("Invalid user data");
        }
        return userRepository.save(user);
    }

    public User updateUser(String id, User user) {
        user.setId(id);
        if (user.getId() == null || user.getName() == null || user.getEmail() == null) {
            throw new IllegalArgumentException("Invalid user data");
        }
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found");
        }
        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found");
        }
        userRepository.deleteById(id);
    }
}
