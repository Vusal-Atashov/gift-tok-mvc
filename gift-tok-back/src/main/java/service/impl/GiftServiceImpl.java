package service.impl;

import domain.entity.Gift;
import domain.repository.GiftRepository;
import service.GiftService;

import java.util.List;
import java.util.Optional;

public class GiftServiceImpl implements GiftService {

    private final GiftRepository giftRepository;

    public GiftServiceImpl(GiftRepository giftRepository) {
        this.giftRepository = giftRepository;
    }

    // Save gift details
    public void saveGift(Gift gift) {
        giftRepository.save(gift);
    }


    public List<Gift> getGiftsForUser(long userId) {
        return giftRepository.findAllByUserId(userId);
    }


    public Optional<Gift> findGiftById(Long id) {
        return giftRepository.findById(id);
    }


    public void deleteGift(Long id) {
        giftRepository.deleteById(id);
    }
}
