package com.gemini.api.persistence;

import com.gemini.api.ui.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single conversation thread, including its history and API session state.
 */
public class Conversation {
    private String id;
    private String title;
    private List<ChatMessage> messages;

    // API Session State
    private String cid; // Conversation ID from API
    private String rid; // Reply ID from API
    private String rcid; // Reply Candidate ID from API

    public Conversation() {
        this.id = UUID.randomUUID().toString();
        this.title = "New Conversation";
        this.messages = new ArrayList<>();
    }

    // Getters and Setters
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

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public String getRcid() {
        return rcid;
    }

    public void setRcid(String rcid) {
        this.rcid = rcid;
    }
}
