package com.gemini.api.client;

import com.gemini.api.client.models.Candidate;
import com.gemini.api.client.models.GeneratedImage;
import com.gemini.api.client.models.ModelOutput;
import com.gemini.api.client.models.WebImage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiClient {

    private static final String GENERATE_URL = "https://gemini.google.com/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate";
    private static final String UPLOAD_URL = "https://content-push.googleapis.com/upload";

    private final String secure1psid;
    private final String secure1psidts;
    private final OkHttpClient httpClient;
    private String accessToken;

    public GeminiClient(String secure1psid, String secure1psidts) {
        this.secure1psid = secure1psid;
        this.secure1psidts = secure1psidts;
        this.httpClient = new OkHttpClient.Builder().build();
    }

    public void init() throws IOException {
        Request request = new Request.Builder()
                .url("https://gemini.google.com")
                .header("Cookie", "__Secure-1PSID=" + secure1psid + "; __Secure-1PSIDTS=" + secure1psidts)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to connect to Gemini. Status code: " + response.code());
            }
            String responseBody = response.body().string();
            Pattern pattern = Pattern.compile("\"SNlM0e\":\"(.*?)\"");
            Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find()) {
                this.accessToken = matcher.group(1);
            } else {
                throw new IOException("Failed to find access token (SNlM0e) in response. The cookies might be invalid or expired.");
            }
        }
    }

    public ChatSession startChat() {
        return new ChatSession(this);
    }

    public ModelOutput generateContent(String prompt, List<File> files) throws IOException {
        return generateContent(prompt, files, null);
    }

    public ModelOutput generateContent(String prompt, List<File> files, ChatSession chatSession) throws IOException {
        if (accessToken == null) {
            throw new IllegalStateException("Client is not initialized. Please call init() first.");
        }

        Gson gson = new Gson();

        List<Object> promptAndFiles = new ArrayList<>();
        promptAndFiles.add(prompt);

        if (files != null && !files.isEmpty()) {
            promptAndFiles.add(0);
            promptAndFiles.add(null);
            List<List<Object>> fileDataList = new ArrayList<>();
            for (File file : files) {
                String uploadedFileId = uploadFile(file);
                List<Object> fileInfo = new ArrayList<>();
                fileInfo.add(Collections.singletonList(uploadedFileId));
                fileInfo.add(file.getName());
                fileDataList.add(fileInfo);
            }
            promptAndFiles.add(fileDataList);
        }

        List<Object> innerList = new ArrayList<>();
        innerList.add(promptAndFiles);
        innerList.add(null);

        if (chatSession != null && chatSession.getCid() != null) {
            List<String> metadata = new ArrayList<>();
            metadata.add(chatSession.getCid());
            metadata.add(chatSession.getRid());
            metadata.add(chatSession.getRcid());
            innerList.add(metadata);
        } else {
            innerList.add(null);
        }

        List<Object> outerList = new ArrayList<>();
        outerList.add(null);
        outerList.add(gson.toJson(innerList));
        String f_req_value = gson.toJson(outerList);

        RequestBody formBody = new FormBody.Builder().add("at", this.accessToken).add("f.req", f_req_value).build();
        Request request = new Request.Builder()
                .url(GENERATE_URL)
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .header("Cookie", "__Secure-1PSID=" + secure1psid + "; __Secure-1PSIDTS=" + secure1psidts)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed with status code: " + response.code());
            }
            String responseBody = response.body().string();
            String[] lines = responseBody.split("\n");
            if (lines.length < 3) {
                throw new IOException("Invalid response format from API.");
            }
            String mainContentJson = lines[2];
            JsonArray responseJson = JsonParser.parseString(mainContentJson).getAsJsonArray();
            JsonArray body = null;
            for (JsonElement partElement : responseJson) {
                try {
                    JsonArray part = partElement.getAsJsonArray();
                    if (part.size() > 2 && !part.get(2).isJsonNull()) {
                        String mainPartJson = part.get(2).getAsString();
                        JsonArray mainPart = JsonParser.parseString(mainPartJson).getAsJsonArray();
                        if (mainPart.size() > 4 && !mainPart.get(4).isJsonNull() && mainPart.get(4).getAsJsonArray().size() > 0) {
                            body = mainPart;
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Ignore malformed parts
                }
            }
            if (body == null) {
                throw new IOException("Could not find the main content body in the API response.");
            }
            List<Candidate> candidates = new ArrayList<>();
            JsonArray candidatesArray = body.get(4).getAsJsonArray();
            for (JsonElement candidateElement : candidatesArray) {
                JsonArray candidateData = candidateElement.getAsJsonArray();
                String rcid = candidateData.get(0).getAsString();
                String text = candidateData.get(1).getAsJsonArray().get(0).getAsString();
                List<WebImage> webImages = new ArrayList<>();
                if (candidateData.size() > 12 && !candidateData.get(12).isJsonNull() && candidateData.get(12).isJsonArray()) {
                    JsonArray c12 = candidateData.get(12).getAsJsonArray();
                    if (c12.size() > 1 && !c12.get(1).isJsonNull() && c12.get(1).isJsonArray()) {
                        JsonArray webImagesArray = c12.get(1).getAsJsonArray();
                        for (JsonElement webImageElement : webImagesArray) {
                            try {
                                JsonArray webImageData = webImageElement.getAsJsonArray();
                                String url = webImageData.get(0).getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString();
                                String title = webImageData.get(7).getAsJsonArray().get(0).getAsString();
                                String alt = webImageData.get(0).getAsJsonArray().get(4).getAsString();
                                webImages.add(new WebImage(url, title, alt));
                            } catch (Exception e) {
                                // Ignore malformed web image data
                            }
                        }
                    }
                }
                candidates.add(new Candidate(rcid, text, null, webImages, new ArrayList<>()));
            }
            if (candidates.isEmpty()) {
                throw new IOException("No candidates found in the response.");
            }
            JsonArray metadataArray = body.get(1).getAsJsonArray();
            List<String> metadata = new ArrayList<>();
            for (JsonElement metaElement : metadataArray) {
                metadata.add(metaElement.getAsString());
            }
            return new ModelOutput(metadata, candidates);
        }
    }

    public String uploadFile(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse("application/octet-stream")))
                .build();
        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .header("Push-ID", "feeds/mcudyrk2a4khkz")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("File upload failed with status code: " + response.code());
            }
            return response.body().string();
        }
    }
}
