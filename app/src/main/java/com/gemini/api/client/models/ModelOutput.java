package com.gemini.api.client.models;

import java.util.List;

public class ModelOutput {
    private List<String> metadata;
    private List<Candidate> candidates;
    private int chosen;

    public ModelOutput(List<String> metadata, List<Candidate> candidates) {
        this.metadata = metadata;
        this.candidates = candidates;
        this.chosen = 0; // Default to the first candidate
    }

    // Getters for properties similar to the Python class
    public Candidate getChosenCandidate() {
        if (candidates == null || candidates.isEmpty() || chosen < 0 || chosen >= candidates.size()) {
            return null;
        }
        return candidates.get(chosen);
    }

    public String getText() {
        Candidate chosenCandidate = getChosenCandidate();
        return chosenCandidate != null ? chosenCandidate.getText() : null;
    }

    public List<Image> getImages() {
        Candidate chosenCandidate = getChosenCandidate();
        return chosenCandidate != null ? chosenCandidate.getImages() : null;
    }

    public String getRcid() {
        Candidate chosenCandidate = getChosenCandidate();
        return chosenCandidate != null ? chosenCandidate.getRcid() : null;
    }

    // Standard Getters and Setters
    public List<String> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<String> metadata) {
        this.metadata = metadata;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public int getChosen() {
        return chosen;
    }

    public void setChosen(int chosen) {
        this.chosen = chosen;
    }
}
