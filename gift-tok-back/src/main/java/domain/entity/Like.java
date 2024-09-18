package domain.entity;

import java.util.Objects;

public class Like {
    private Long id;
    private Long userId;
    private Long likeCount;

    public Like(Long id, Long userId, Long likeCount) {
        this.id = id;
        this.userId = userId;
        this.likeCount = likeCount;
    }

    public Like(Long userId, Long likeCount) {
        this.userId = userId;
        this.likeCount = likeCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Like like = (Like) o;
        return Objects.equals(id, like.id) && Objects.equals(userId, like.userId) && Objects.equals(likeCount, like.likeCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, likeCount);
    }

    @Override
    public String toString() {
        return "Like{" +
                "id=" + id +
                ", userId=" + userId +
                ", likeCount=" + likeCount +
                '}';
    }
}
