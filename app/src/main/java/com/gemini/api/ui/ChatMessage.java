package com.gemini.api.ui;

/**
 * Represents a single message in the chat interface.
 */
public class ChatMessage {

    public enum Sender {
        USER, GEMINI
    }

    private final String text;
    private final Sender sender;
    // TODO: Add support for images and other content types later.

    public ChatMessage(String text, Sender sender) {
        this.text = text;
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public Sender getSender() {
        return sender;
    }
}
