package com.gemini.api.client.utils;

import com.gemini.api.client.models.Image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Utility class for handling images.
 */
public class ImageUtils {

    /**
     * Downloads an image and saves it to a destination file.
     *
     * @param image The Image object containing the URL to download.
     * @param destination The File where the image will be saved.
     * @param client The OkHttpClient to use for the download.
     * @throws IOException if the download or file write fails.
     */
    public static void save(Image image, File destination, OkHttpClient client) throws IOException {
        Request request = new Request.Builder().url(image.getUrl()).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download image. Status code: " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Response body for image download is null.");
            }

            // Ensure parent directories exist
            File parentDir = destination.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("Could not create parent directories for destination file.");
                }
            }

            try (InputStream inputStream = body.byteStream();
                 FileOutputStream outputStream = new FileOutputStream(destination)) {

                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
