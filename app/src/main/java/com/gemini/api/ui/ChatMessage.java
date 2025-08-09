package com.gemini.api.ui;

import com.gemini.api.client.models.Image;
import java.util.List;

/**
 * Represents a single message in the chat interface.
 */
public class ChatMessage {

    public enum Sender {
        USER, GEMINI
    }

    private final String text;
    private final Sender sender;
    private final List<Image> images;

    public ChatMessage(String text, Sender sender, List<Image> images) {
        this.text = text;
        this.sender = sender;
        this.images = images;
    }

    public ChatMessage(String text, Sender sender) {
        this(text, sender, null);
    }

    public String getText() {
        return text;
    }

    public Sender getSender() {
        return sender;
    }

    public List<Image> getImages() {
        return images;
    }
}
