package com.dotsoft.smartsonia;

/**
 * SMART SONIA USER CLASS
 */
public class User {

    private String  userName, login, userId, accessToken;

    public User(String userName, String lastName, String firstName, String userId, String accessToken,
                String login){
        this.userName = userName;
        this.login = login;
        this.userId = userId;
        this.accessToken = accessToken;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserId() {
        return userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public boolean isLogin() {
        return login.equals("IN");
    }
}