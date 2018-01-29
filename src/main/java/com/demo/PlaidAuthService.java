package com.demo;

import org.springframework.stereotype.Service;


/**
 * Simple service to store user data. Only for demonstration purposes.
 */
@Service
public class PlaidAuthService {

    private String accessToken;
    private String itemId;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
}
