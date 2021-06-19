package models;

import java.nio.charset.StandardCharsets;

public class User {

    private int id;
    private final String login;
    private final byte[] password;

    public User() {
        this.login = "login";
        this.password = "password".getBytes(StandardCharsets.UTF_8);
    }

    public User(String login, byte[] password) {
        this.login = login;
        this.password = password;
    }

    public User(int id, String login, byte[] password) {
        this.id = id;
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public byte[] getPassword() {
        return password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "models.User{" +
                "login='" + login + '\'' +
                ", password='" + password.toString() + '\'' +
                '}';
    }
}
