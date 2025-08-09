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
     * Constructor is package-private. ChatSessions should be created via {@link GeminiClient#startChat()}.
     * @param client The GeminiClient instance to use for communication.
     */
    ChatSession(GeminiClient client) {
        this.client = client;
    }

    /**
     * Sends a message in the context of this conversation.
     * @param prompt The text prompt.
     * @param files A list of files to attach. Can be null or empty.
     * @return The model's response.
     * @throws IOException if the network request fails.
     */
    public ModelOutput sendMessage(String prompt, List<File> files) throws IOException {
        // Call the client's generateContent method, passing this session object.
        ModelOutput output = client.generateContent(prompt, files, this);

        // After a successful response, update the session's metadata.
        if (output != null && output.getMetadata() != null) {
            List<String> metadata = output.getMetadata();
            if (metadata.size() > 0) this.cid = metadata.get(0);
            if (metadata.size() > 1) this.rid = metadata.get(1);
            // The rcid for the *next* request is the one from the chosen candidate of the *current* response.
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
