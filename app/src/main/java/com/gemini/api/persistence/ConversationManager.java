package com.gemini.api.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the saving and loading of chat conversations using SharedPreferences.
 */
public class ConversationManager {

    private static final String PREFS_NAME = "GeminiConversations";
    private static final String KEY_CONVERSATIONS = "conversations_map";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public ConversationManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Saves a conversation. If a conversation with the same ID exists, it will be updated.
     * @param conversation The conversation to save.
     */
    public void saveConversation(Conversation conversation) {
        Map<String, String> conversations = getAllConversationsAsJson();
        String conversationJson = gson.toJson(conversation);
        conversations.put(conversation.getId(), conversationJson);
        saveConversationsMap(conversations);
    }

    /**
     * Retrieves all conversations.
     * @return A list of all saved conversations.
     */
    public List<Conversation> getConversations() {
        Map<String, String> conversationsJson = getAllConversationsAsJson();
        List<Conversation> conversations = new ArrayList<>();
        for (String json : conversationsJson.values()) {
            conversations.add(gson.fromJson(json, Conversation.class));
        }
        return conversations;
    }

    /**
     * Deletes a conversation by its ID.
     * @param conversationId The ID of the conversation to delete.
     */
    public void deleteConversation(String conversationId) {
        Map<String, String> conversations = getAllConversationsAsJson();
        if (conversations.containsKey(conversationId)) {
            conversations.remove(conversationId);
            saveConversationsMap(conversations);
        }
    }

    private Map<String, String> getAllConversationsAsJson() {
        String json = sharedPreferences.getString(KEY_CONVERSATIONS, null);
        if (json == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<HashMap<String, String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    private void saveConversationsMap(Map<String, String> conversations) {
        String json = gson.toJson(conversations);
        sharedPreferences.edit().putString(KEY_CONVERSATIONS, json).apply();
    }
}
