package domain.entity;

import java.util.Objects;

public class Gift {
    private Long id;
    private Long userId;
    private int giftPrice;
    private String giftName;
    private Integer giftCount; // Using Integer for consistency with other wrapper classes

    public Gift(Long id, Long userId, int giftPrice, String giftName, Integer giftCount) {
        this.id = id;
        this.userId = userId;
        this.giftPrice = giftPrice;
        this.giftName = giftName;
        this.giftCount = giftCount;
    }

    public Gift(Long userId, int giftPrice, String giftName, Integer giftCount) {
        // Constructor for creating a new gift without an ID (e.g., before saving to DB)
        this.userId = userId;
        this.giftPrice = giftPrice;
        this.giftName = giftName;
        this.giftCount = giftCount;
    }

    // Getters and Setters
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

    public int getGiftPrice() {
        return giftPrice;
    }

    public void setGiftPrice(int giftPrice) {
        this.giftPrice = giftPrice;
    }

    public String getGiftName() {
        return giftName;
    }

    public void setGiftName(String giftName) {
        this.giftName = giftName;
    }

    public Integer getGiftCount() {
        return giftCount;
    }

    public void setGiftCount(Integer giftCount) {
        this.giftCount = giftCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gift gift = (Gift) o;
        return Objects.equals(id, gift.id) &&
                Objects.equals(userId, gift.userId) &&
                Objects.equals(giftPrice, gift.giftPrice) &&
                Objects.equals(giftName, gift.giftName) &&
                Objects.equals(giftCount, gift.giftCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, giftPrice, giftName, giftCount);
    }

    @Override
    public String toString() {
        return "Gift{" +
                "id=" + id +
                ", userId=" + userId +
                ", giftPrice=" + giftPrice +
                ", giftName='" + giftName + '\'' +
                ", giftCount=" + giftCount +
                '}';
    }
}
