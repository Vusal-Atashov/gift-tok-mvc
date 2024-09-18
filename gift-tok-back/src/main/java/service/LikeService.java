package service;

import domain.entity.Like;

public interface LikeService {
    void saveLike(Like like);
    Like findLikeById(long id);
    void updateLike(Like like);
    void deleteLike(Like like);
}
