package com.gemini.api.client;

import com.gemini.api.client.models.ModelOutput;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Represents a stateful conversation with the Gemini API.
 * This class holds the necessary metadata to continue a conversation across multiple turns.
 */
public class ChatSession {
    private final GeminiClient client;
    private String cid;
    private String rid;
    private String rcid;

    /**
     * Constructor for starting a new chat.
     * @param client The GeminiClient instance to use for communication.
     */
    ChatSession(GeminiClient client) {
        this.client = client;
    }

    /**
     * Constructor for continuing an existing chat from persisted metadata.
     * @param client The GeminiClient instance.
     * @param cid The existing conversation ID.
     * @param rid The existing reply ID.
     * @param rcid The existing reply candidate ID.
     */
    ChatSession(GeminiClient client, String cid, String rid, String rcid) {
        this.client = client;
        this.cid = cid;
        this.rid = rid;
        this.rcid = rcid;
    }

    /**
     * Sends a message in the context of this conversation.
     * @param prompt The text prompt.
     * @param files A list of files to attach. Can be null or empty.
     * @return The model's response.
     * @throws IOException if the network request fails.
     */
    public ModelOutput sendMessage(String prompt, List<File> files) throws IOException {
        ModelOutput output = client.generateContent(prompt, files, this);
        if (output != null && output.getMetadata() != null) {
            List<String> metadata = output.getMetadata();
            if (metadata.size() > 0) this.cid = metadata.get(0);
            if (metadata.size() > 1) this.rid = metadata.get(1);
            this.rcid = output.getRcid();
        }
        return output;
    }

    /**
     * Convenience method to send a text-only message.
     * @param prompt The text prompt.
     * @return The model's response.
     * @throws IOException if the network request fails.
     */
    public ModelOutput sendMessage(String prompt) throws IOException {
        return sendMessage(prompt, null);
    }

    // Getters for metadata
    public String getCid() { return cid; }
    public String getRid() { return rid; }
    public String getRcid() { return rcid; }
}
