package service;

import domain.entity.Gift;
import java.util.List;
import java.util.Optional;

public interface GiftService {

    // Save a gift to the repository
    void saveGift(Gift gift);

    // Retrieve all gifts for a given user
    List<Gift> getGiftsForUser(long userId);

    // Find a gift by its ID
    Optional<Gift> findGiftById(Long id);

    // Delete a gift by its ID
    void deleteGift(Long id);
}
