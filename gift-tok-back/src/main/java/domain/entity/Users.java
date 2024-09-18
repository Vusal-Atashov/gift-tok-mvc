package domain.entity;

import java.util.Objects;

public class Users {
    private Long id;
    private String userName;
    private String profileName;
    private String pictureBase64;


    public Users(String name, String profileName, String pictureBase64) {
        this.userName = name;
        this.profileName = profileName;
        this.pictureBase64 = pictureBase64;
    }

    public Users(Long id, String userName, String profileName, String pictureBase64) {
        this.id = id;
        this.userName = userName;
        this.profileName = profileName;
        this.pictureBase64 = pictureBase64;
    }

    public Users() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPictureBase64() {
        return pictureBase64;
    }

    public void setPictureBase64(String pictureBase64) {
        this.pictureBase64 = pictureBase64;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Users users = (Users) o;
        return Objects.equals(id, users.id) && Objects.equals(userName, users.userName) && Objects.equals(profileName, users.profileName) && Objects.equals(pictureBase64, users.pictureBase64);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userName, profileName, pictureBase64);
    }

    @Override
    public String toString() {
        return "Users{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", profileName='" + profileName + '\'' +
                ", pictureBase64='" + pictureBase64 + '\'' +
                '}';
    }
}

