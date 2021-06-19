package models;

public class AuthUser {
    private String login;
    private String passwordBase64;

    public AuthUser() { login = "log"; passwordBase64 = "sfs"; }

    public AuthUser(String login, String passwordBase64) {
        this.login = login;
        this.passwordBase64 = passwordBase64;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPasswordBase64() {
        return passwordBase64;
    }

    public void setPasswordBase64(String passwordBase64) {
        this.passwordBase64 = passwordBase64;
    }
}
