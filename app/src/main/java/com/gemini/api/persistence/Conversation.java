package com.gemini.api.persistence;

import com.gemini.api.ui.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single conversation thread, including its history.
 */
public class Conversation {
    private String id;
    private String title;
    private List<ChatMessage> messages;

    public Conversation() {
        this.id = UUID.randomUUID().toString();
        this.title = "New Conversation";
        this.messages = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
}
