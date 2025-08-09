package com.gemini.api;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private File attachedFile;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupFilePicker();
        setupToolbarAndDrawer();
        setupChatRecyclerView();
        setupInputControls();

        conversationManager = new ConversationManager(this);
        loadConversations();
        initializeClient();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow(); // Shut down the background executor
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
                // Schedule cookie rotation after successful init
                scheduleCookieRotation();
            } catch (IOException e) {
                handler.post(() -> Toast.makeText(MainActivity.this, "Client Init Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void scheduleCookieRotation() {
        executor.scheduleAtFixedRate(() -> {
            try {
                geminiClient.rotateCookies();
                handler.post(() -> Toast.makeText(MainActivity.this, "Session cookie rotated.", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                // Silently fail or show a non-intrusive log
                e.printStackTrace();
            }
        }, 10, 10, TimeUnit.MINUTES);
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                attachedFile = getFileFromUri(uri);
                                Toast.makeText(this, "Attached: " + attachedFile.getName(), Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Toast.makeText(this, "Failed to attach file.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void setupInputControls() {
        Button sendButton = findViewById(R.id.button_send);
        ImageButton attachButton = findViewById(R.id.button_attach);
        EditText messageEditText = findViewById(R.id.edit_text_message);

        attachButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        });

        sendButton.setOnClickListener(v -> {
            String text = messageEditText.getText().toString().trim();
            if (text.isEmpty() && attachedFile == null) return;
            if (geminiClient == null || currentChatSession == null) {
                Toast.makeText(this, "Client not ready or no active chat.", Toast.LENGTH_LONG).show();
                return;
            }

            addMessageToChat(new ChatMessage(text, ChatMessage.Sender.USER));
            messageEditText.setText("");
            Toast.makeText(this, "Generating response...", Toast.LENGTH_SHORT).show();

            final List<File> filesToSend = attachedFile != null ? Collections.singletonList(attachedFile) : null;
            attachedFile = null;

            executor.execute(() -> {
                try {
                    ModelOutput response = currentChatSession.sendMessage(text, filesToSend);
                    handler.post(() -> {
                        if (response != null && response.getText() != null) {
                            ChatMessage geminiMessage = new ChatMessage(response.getText(), ChatMessage.Sender.GEMINI, response.getImages());
                            addMessageToChat(geminiMessage);
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

    private File getFileFromUri(Uri uri) throws IOException {
        String fileName = getFileName(uri);
        File tempFile = new File(getCacheDir(), fileName);
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
        return tempFile;
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                       result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
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
            loadChat(conversations.get(conversations.size() - 1));
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
