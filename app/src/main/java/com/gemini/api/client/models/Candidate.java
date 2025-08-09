package com.gemini.api.client.models;

import java.util.ArrayList;
import java.util.List;

public class Candidate {
    private String rcid;
    private String text;
    private String thoughts;
    private List<WebImage> webImages;
    private List<GeneratedImage> generatedImages;

    public Candidate(String rcid, String text, String thoughts, List<WebImage> webImages, List<GeneratedImage> generatedImages) {
        this.rcid = rcid;
        this.text = text;
        this.thoughts = thoughts;
        this.webImages = webImages != null ? webImages : new ArrayList<>();
        this.generatedImages = generatedImages != null ? generatedImages : new ArrayList<>();
    }

    // Method to combine both lists of images, similar to the Python property
    public List<Image> getImages() {
        List<Image> allImages = new ArrayList<>();
        allImages.addAll(webImages);
        allImages.addAll(generatedImages);
        return allImages;
    }

    // Getters
    public String getRcid() {
        return rcid;
    }

    public String getText() {
        return text;
    }

    public String getThoughts() {
        return thoughts;
    }

    public List<WebImage> getWebImages() {
        return webImages;
    }

    public List<GeneratedImage> getGeneratedImages() {
        return generatedImages;
    }

    // Setters
    public void setRcid(String rcid) {
        this.rcid = rcid;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setThoughts(String thoughts) {
        this.thoughts = thoughts;
    }

    public void setWebImages(List<WebImage> webImages) {
        this.webImages = webImages;
    }

    public void setGeneratedImages(List<GeneratedImage> generatedImages) {
        this.generatedImages = generatedImages;
    }
}
