package service.impl;

import domain.entity.Like;
import domain.repository.LikeRepository;
import service.LikeService;

import java.sql.Connection;
import java.sql.SQLException;

public class LikeServiceImpl implements LikeService {
    private final LikeRepository likeRepository;

    public LikeServiceImpl(LikeRepository likeRepository) {
        this.likeRepository = likeRepository;
    }

    @Override
    public void saveLike(Like like) {
        try (Connection conn = likeRepository.getConnection()) {
            conn.setAutoCommit(false);  // Transaction başlat

            // Mevcut like'ı bul
            Like existingLike = likeRepository.findByUserId(like.getUserId(), conn);

            if (existingLike != null) {
                // Eğer mevcut like varsa, yeni like sayısını ekleyerek güncelle
                long updatedLikeCount = existingLike.getLikeCount() + like.getLikeCount();
                existingLike.setLikeCount(updatedLikeCount);
                likeRepository.update(existingLike, conn);  // Güncellenmiş metod
            } else {
                // Eğer like kaydı yoksa, yeni bir like kaydet
                likeRepository.save(like, conn);
            }

            conn.commit();  // İşlemleri onayla (commit)
        } catch (SQLException e) {
            e.printStackTrace();
            // Bu noktada, SQLException işlenebilir ve uygun şekilde ele alınabilir.
        }
    }

    @Override
    public Like findLikeById(long id) {
        return likeRepository.findById(id);
    }

    @Override
    public void updateLike(Like like) {
        try (Connection conn = likeRepository.getConnection()) {
            likeRepository.update(like, conn);  // Güncellenmiş metod
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteLike(Like like) {
        try (Connection conn = likeRepository.getConnection()) {
            likeRepository.delete(like, conn);  // delete metodunu güncelle
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
