package com.gemini.api.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gemini.api.R;
import com.gemini.api.client.models.Image;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_GEMINI = 2;

    private final List<ChatMessage> messages;
    private final ExecutorService imageLoaderExecutor = Executors.newFixedThreadPool(4); // For loading images

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.getSender() == ChatMessage.Sender.USER) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_GEMINI;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_USER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_gemini, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageBody;
        private final ImageView imageAttachment;
        private final Handler handler = new Handler(Looper.getMainLooper());

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.text_message_body);
            // Image view is only in the gemini layout
            imageAttachment = itemView.findViewById(R.id.image_attachment);
        }

        void bind(ChatMessage message) {
            messageBody.setText(message.getText());
            if (imageAttachment != null) {
                if (message.getImages() != null && !message.getImages().isEmpty()) {
                    // For simplicity, just show the first image
                    Image image = message.getImages().get(0);
                    imageAttachment.setVisibility(View.VISIBLE);
                    loadImage(image.getUrl());
                } else {
                    imageAttachment.setVisibility(View.GONE);
                }
            }
        }

        private void loadImage(String urlString) {
            imageLoaderExecutor.execute(() -> {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    handler.post(() -> imageAttachment.setImageBitmap(bitmap));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
