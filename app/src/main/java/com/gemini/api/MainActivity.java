package com.gemini.api;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gemini.api.client.ChatSession;
import com.gemini.api.client.GeminiClient;
import com.gemini.api.client.models.ModelOutput;
import com.gemini.api.persistence.Conversation;
import com.gemini.api.persistence.ConversationManager;
import com.gemini.api.ui.ChatAdapter;
import com.gemini.api.ui.ChatMessage;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView chatRecyclerView;
    private ConversationManager conversationManager;
    private Conversation currentConversation;
    private ChatSession currentChatSession;
    private List<ChatMessage> messages;
    private ChatAdapter chatAdapter;
    private GeminiClient geminiClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbarAndDrawer();
        setupChatRecyclerView();
        setupSendButton();

        conversationManager = new ConversationManager(this);
        loadConversations();
        initializeClient();
    }

    private void initializeClient() {
        SharedPreferences prefs = getSharedPreferences("GeminiPrefs", MODE_PRIVATE);
        String psid = prefs.getString("PSID", "");
        String psidts = prefs.getString("PSIDTS", "");

        if (psid.isEmpty() || psidts.isEmpty()) {
            Toast.makeText(this, "API credentials are not set. Please set them in Settings.", Toast.LENGTH_LONG).show();
            return;
        }

        geminiClient = new GeminiClient(psid, psidts);
        executor.execute(() -> {
            try {
                geminiClient.init();
                handler.post(() -> Toast.makeText(MainActivity.this, "Gemini Client Initialized", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                handler.post(() -> Toast.makeText(MainActivity.this, "Client Init Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void setupSendButton() {
        Button sendButton = findViewById(R.id.button_send);
        EditText messageEditText = findViewById(R.id.edit_text_message);

        sendButton.setOnClickListener(v -> {
            String text = messageEditText.getText().toString().trim();
            if (text.isEmpty() || geminiClient == null || currentChatSession == null) {
                Toast.makeText(this, "Client not ready or no active chat.", Toast.LENGTH_LONG).show();
                return;
            }

            addMessageToChat(new ChatMessage(text, ChatMessage.Sender.USER));
            messageEditText.setText("");
            Toast.makeText(this, "Generating response...", Toast.LENGTH_SHORT).show();

            executor.execute(() -> {
                try {
                    // Pass the active ChatSession to the client
                    ModelOutput response = currentChatSession.sendMessage(text);
                    handler.post(() -> {
                        if (response != null && response.getText() != null) {
                            addMessageToChat(new ChatMessage(response.getText(), ChatMessage.Sender.GEMINI));
                            // Update the persisted conversation with the new API metadata
                            currentConversation.setCid(currentChatSession.getCid());
                            currentConversation.setRid(currentChatSession.getRid());
                            currentConversation.setRcid(currentChatSession.getRcid());
                            conversationManager.saveConversation(currentConversation);
                        } else {
                            Toast.makeText(MainActivity.this, "Received an empty response.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    handler.post(() -> Toast.makeText(MainActivity.this, "API Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            });
        });
    }

    private void setupToolbarAndDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);
    }

    private void setupChatRecyclerView() {
        chatRecyclerView = findViewById(R.id.recycler_view_chat);
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void loadConversations() {
        List<Conversation> conversations = conversationManager.getConversations();
        Menu menu = navigationView.getMenu();
        menu.removeGroup(R.id.group_conversations);
        if (conversations.isEmpty()) {
            startNewChat();
        } else {
            for (Conversation convo : conversations) {
                menu.add(R.id.group_conversations, convo.getId().hashCode(), Menu.NONE, convo.getTitle()).setCheckable(true);
            }
            loadChat(conversations.get(conversations.size() - 1)); // Load the most recent chat
        }
    }

    private void startNewChat() {
        currentConversation = new Conversation();
        conversationManager.saveConversation(currentConversation);
        loadChat(currentConversation);
        loadConversations();
    }

    private void loadChat(Conversation conversation) {
        currentConversation = conversation;
        // Create a new ChatSession for the loaded conversation
        currentChatSession = new ChatSession(geminiClient, conversation.getCid(), conversation.getRid(), conversation.getRcid());

        messages.clear();
        messages.addAll(conversation.getMessages());
        chatAdapter.notifyDataSetChanged();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(conversation.getTitle());
        }
    }

    private void addMessageToChat(ChatMessage message) {
        messages.add(message);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.scrollToPosition(messages.size() - 1);

        if (currentConversation.getMessages().isEmpty()) {
            currentConversation.setTitle(message.getText());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(currentConversation.getTitle());
            }
        }
        currentConversation.getMessages().add(message);
        conversationManager.saveConversation(currentConversation);

        if (currentConversation.getMessages().size() == 1) {
            loadConversations();
        }
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        if (item.getItemId() == R.id.nav_new_chat) {
            startNewChat();
        } else if (item.getItemId() == R.id.nav_settings) {
            showSettingsDialog();
        } else {
            List<Conversation> conversations = conversationManager.getConversations();
            for (Conversation convo : conversations) {
                if (convo.getId().hashCode() == item.getItemId()) {
                    loadChat(convo);
                    break;
                }
            }
        }
        return true;
    }

    private void showSettingsDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        TextInputEditText psidInput = dialogView.findViewById(R.id.edit_text_psid);
        TextInputEditText psidtsInput = dialogView.findViewById(R.id.edit_text_psidts);
        SharedPreferences prefs = getSharedPreferences("GeminiPrefs", MODE_PRIVATE);
        psidInput.setText(prefs.getString("PSID", ""));
        psidtsInput.setText(prefs.getString("PSIDTS", ""));
        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    prefs.edit()
                         .putString("PSID", psidInput.getText().toString())
                         .putString("PSIDTS", psidtsInput.getText().toString())
                         .apply();
                    initializeClient();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
