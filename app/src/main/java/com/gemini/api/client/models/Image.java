package com.gemini.api.client.models;

public class Image {
    private String url;
    private String title;
    private String alt;

    public Image(String url, String title, String alt) {
        this.url = url;
        this.title = title;
        this.alt = alt;
    }

    // Getters
    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getAlt() {
        return alt;
    }

    // Setters
    public void setUrl(String url) {
        this.url = url;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    @Override
    public String toString() {
        return "Image{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", alt='" + alt + '\'' +
                '}';
    }
}
