package com.gemini.api.client.models;

public class Gem {
    private String id;
    private String name;
    private String description;
    private String prompt;
    private boolean predefined;

    public Gem(String id, String name, String description, String prompt, boolean predefined) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.prompt = prompt;
        this.predefined = predefined;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPrompt() {
        return prompt;
    }

    public boolean isPredefined() {
        return predefined;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void setPredefined(boolean predefined) {
        this.predefined = predefined;
    }
}
