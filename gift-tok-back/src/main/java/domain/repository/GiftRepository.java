package domain.repository;

import domain.entity.Gift;
import java.util.List;
import java.util.Optional;

public interface GiftRepository {

    // Save a gift to the repository
    void save(Gift gift);

    // Find a gift by its ID
    Optional<Gift> findById(Long id);

    // Find all gifts for a particular user
    List<Gift> findAllByUserId(long userId);

    // Delete a gift by its ID
    void deleteById(Long id);
}
