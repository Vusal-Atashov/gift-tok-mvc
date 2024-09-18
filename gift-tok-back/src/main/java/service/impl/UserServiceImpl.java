package service.impl;

import domain.entity.Users;
import domain.repository.UserRepository;
import domain.repository.impl.UserRepositoryImpl;
import service.UserService;

import java.util.List;

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Users saveUser(Users user) {
        return userRepository.save(user);
    }

    @Override
    public Users findUserByName(String name) {
        return userRepository.findByName(name);
    }

    @Override
    public Users getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Users getUserByName(String name) {
        return userRepository.findByName(name);
    }

    @Override
    public void deleteUser(Long id) {
        Users user = userRepository.findById(id);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Override
    public void updateUser(Users user) {
        userRepository.update(user);
    }

    @Override
    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }


}
