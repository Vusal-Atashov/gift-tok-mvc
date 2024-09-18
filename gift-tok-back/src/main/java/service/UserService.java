package service;

import domain.entity.Users;
import java.util.List;

public interface UserService {
    Users saveUser(Users user);

    Users findUserByName(String name);

    Users getUserById(Long id);

    Users getUserByName(String name);

    void deleteUser(Long id);

    void updateUser(Users user);

    List<Users> getAllUsers();
}
