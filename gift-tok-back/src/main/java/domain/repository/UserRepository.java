package domain.repository;

import domain.entity.Users;
import java.util.List;

public interface UserRepository {
    Users save(Users user);

    Users findById(Long id);

    Users findByName(String name);

    void delete(Users user);

    void update(Users user);

    // Tüm kullanıcıları döndüren metot
    List<Users> findAll();
}
