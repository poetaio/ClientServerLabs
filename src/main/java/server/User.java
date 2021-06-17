package server;

public class User {
    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    private String login;
    private String password;

    public User() {
        this.login = "login";
        this.password = "password";
    }

    public User(String login, String password) {
        this.login = login;
        this.password = password;
    }

    @Override
    public String toString() {
        return "server.User{" +
                "login='" + login + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
