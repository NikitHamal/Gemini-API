package com.gemini.api.client.models;

import java.util.Map;

public class GeneratedImage extends Image {
    private Map<String, String> cookies;

    public GeneratedImage(String url, String title, String alt, Map<String, String> cookies) {
        super(url, title, alt);
        this.cookies = cookies;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }
}
