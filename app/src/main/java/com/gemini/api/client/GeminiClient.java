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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiClient {

    private static final String BASE_URL = "https://gemini.google.com/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate";
    private static final String UPLOAD_URL = "https://content-push.googleapis.com/upload";
    private static final String ROTATE_COOKIES_URL = "https://accounts.google.com/RotateCookies";
    private static final String BL_PARAM = "boq_assistant-bard-web-server_20250807.07_p2";
    private static final String F_SID_PARAM = "8540818621995910897";
    private static final String REQ_ID_PARAM = "3902849";
    private static final String RT_PARAM = "c";
    private static final String HL_PARAM = "en";
    private static final Map<String, String> HEADERS = new HashMap<>();
    static {
        HEADERS.put("authority", "gemini.google.com");
        HEADERS.put("accept", "*/*");
        HEADERS.put("accept-language", "en-US,en;q=0.9");
        HEADERS.put("origin", "https://gemini.google.com");
        HEADERS.put("referer", "https://gemini.google.com/");
        HEADERS.put("sec-ch-ua", "\"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"");
        HEADERS.put("sec-ch-ua-mobile", "?1");
        HEADERS.put("sec-ch-ua-platform", "\"Android\"");
        HEADERS.put("sec-fetch-dest", "empty");
        HEADERS.put("sec-fetch-mode", "cors");
        HEADERS.put("sec-fetch-site", "same-origin");
        HEADERS.put("user-agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36");
        HEADERS.put("x-same-domain", "1");
        HEADERS.put("x-goog-ext-525001261-jspb", "[1,null,null,null,\"9ec249fc9ad08861\",null,null,null,[4]]");
    }
    private final String secure1psid;
    private String secure1psidts;
    private final OkHttpClient httpClient;
    private String accessToken;

    public GeminiClient(String secure1psid, String secure1psidts) {
        this.secure1psid = secure1psid;
        this.secure1psidts = secure1psidts;
        this.httpClient = new OkHttpClient.Builder().build();
    }

    public void init() throws IOException {
        String cookieValue = "__Secure-1PSID=" + secure1psid;
        if (secure1psidts != null && !secure1psidts.isEmpty()) {
            cookieValue += "; __Secure-1PSIDTS=" + secure1psidts;
        }
        Request request = new Request.Builder()
                .url("https://gemini.google.com")
                .header("Cookie", cookieValue)
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

    public void rotateCookies() throws IOException {
        RequestBody requestBody = RequestBody.create("[000,\"-0000000000000000000\"]", MediaType.parse("application/json"));
        String cookieValue = "__Secure-1PSID=" + secure1psid;
        if (secure1psidts != null && !secure1psidts.isEmpty()) {
            cookieValue += "; __Secure-1PSIDTS=" + secure1psidts;
        }
        Request request = new Request.Builder()
                .url(ROTATE_COOKIES_URL)
                .post(requestBody)
                .header("Cookie", cookieValue)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Cookie rotation failed with status code: " + response.code());
            }
            List<String> cookieHeaders = response.headers("Set-Cookie");
            for (String header : cookieHeaders) {
                if (header.startsWith("__Secure-1PSIDTS=")) {
                    String[] parts = header.split(";");
                    this.secure1psidts = parts[0].substring("__Secure-1PSIDTS=".length());
                    return;
                }
            }
        }
    }

    public ChatSession startChat() {
        return new ChatSession(this);
    }

    public ModelOutput generateContent(String prompt, List<File> files, ChatSession chatSession) throws IOException {
        if (accessToken == null) {
            throw new IllegalStateException("Client is not initialized. Please call init() first.");
        }
        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                .addQueryParameter("bl", BL_PARAM)
                .addQueryParameter("f.sid", F_SID_PARAM)
                .addQueryParameter("hl", HL_PARAM)
                .addQueryParameter("_reqid", REQ_ID_PARAM)
                .addQueryParameter("rt", RT_PARAM)
                .build();
        String f_req_value = buildFReq(prompt, chatSession, files);
        RequestBody formBody = new FormBody.Builder()
                .add("at", this.accessToken)
                .add("f.req", f_req_value)
                .build();
        Request.Builder requestBuilder = new Request.Builder().url(url);
        for (Map.Entry<String, String> entry : HEADERS.entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }
        String cookieValue = "__Secure-1PSID=" + secure1psid;
        if (secure1psidts != null && !secure1psidts.isEmpty()) {
            cookieValue += "; __Secure-1PSIDTS=" + secure1psidts;
        }
        requestBuilder.addHeader("cookie", cookieValue);
        requestBuilder.post(formBody);
        Request request = requestBuilder.build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response.code() + " " + response.message());
            }
            String responseBody = response.body().string();
            String[] lines = responseBody.split("\n");
            if (lines.length < 3) {
                throw new IOException("Invalid response format from API.");
            }
            String mainContentJson = lines[2];
            JsonArray responseJson = JsonParser.parseString(mainContentJson).getAsJsonArray();
            int bodyIndex = findBodyIndex(responseJson);
            if (bodyIndex == -1) {
                throw new IOException("Could not find the main content body in the API response.");
            }
            JsonArray body = JsonParser.parseString(responseJson.get(bodyIndex).getAsJsonArray().get(2).getAsString()).getAsJsonArray();
            List<Candidate> candidates = new ArrayList<>();
            JsonArray candidatesArray = body.get(4).getAsJsonArray();
            for (int i = 0; i < candidatesArray.size(); i++) {
                JsonArray candidateData = candidatesArray.get(i).getAsJsonArray();
                String rcid = candidateData.get(0).getAsString();
                String text = candidateData.get(1).getAsJsonArray().get(0).getAsString();
                String thoughts = null;
                try {
                    if (candidateData.size() > 37 && !candidateData.get(37).isJsonNull()) {
                        thoughts = candidateData.get(37).getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString();
                    }
                } catch (Exception e) { /* Ignore */ }
                List<WebImage> webImages = new ArrayList<>();
                List<GeneratedImage> generatedImages = new ArrayList<>();
                if (candidateData.size() > 12 && !candidateData.get(12).isJsonNull() && candidateData.get(12).isJsonArray()) {
                    JsonArray c12 = candidateData.get(12).getAsJsonArray();
                    if (c12.size() > 1 && !c12.get(1).isJsonNull() && c12.get(1).isJsonArray()) {
                        for (JsonElement webImageElement : c12.get(1).getAsJsonArray()) {
                            try {
                                JsonArray webImageData = webImageElement.getAsJsonArray();
                                String imageUrl = webImageData.get(0).getAsJsonArray().get(0).getAsJsonArray().get(0).getAsString();
                                String imageTitle = webImageData.get(7).getAsJsonArray().get(0).getAsString();
                                String imageAlt = webImageData.get(0).getAsJsonArray().get(4).getAsString();
                                webImages.add(new WebImage(imageUrl, imageTitle, imageAlt));
                            } catch (Exception e) { /* Ignore */ }
                        }
                    }
                    if (c12.size() > 7 && !c12.get(7).isJsonNull() && c12.get(7).isJsonArray() && !c12.get(7).getAsJsonArray().isEmpty()) {
                        JsonArray imgBody = findImageBody(responseJson, bodyIndex, i);
                        if (imgBody != null) {
                            JsonArray imgCandidate = imgBody.get(4).getAsJsonArray().get(i).getAsJsonArray();
                            JsonArray generatedImagesArray = imgCandidate.get(12).getAsJsonArray().get(7).getAsJsonArray().get(0).getAsJsonArray();
                            for (JsonElement generatedImageElement : generatedImagesArray) {
                                try {
                                    JsonArray generatedImageData = generatedImageElement.getAsJsonArray();
                                    String genImageUrl = generatedImageData.get(0).getAsJsonArray().get(3).getAsJsonArray().get(3).getAsString();
                                    String genImageTitle = "[Generated Image " + generatedImageData.get(3).getAsJsonArray().get(6).getAsString() + "]";
                                    String genImageAlt = generatedImageData.get(3).getAsJsonArray().get(5).getAsJsonArray().get(0).getAsString();
                                    Map<String, String> cookieMap = new HashMap<>();
                                    cookieMap.put("__Secure-1PSID", this.secure1psid);
                                    if (this.secure1psidts != null) cookieMap.put("__Secure-1PSIDTS", this.secure1psidts);
                                    generatedImages.add(new GeneratedImage(genImageUrl, genImageTitle, genImageAlt, cookieMap));
                                } catch (Exception e) { /* Ignore */ }
                            }
                        }
                    }
                }
                candidates.add(new Candidate(rcid, text, thoughts, webImages, generatedImages));
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

    private String buildFReq(String prompt, ChatSession chatSession, List<File> files) throws IOException {
        Gson gson = new Gson();
        List<Object> promptAndFilesContainer = new ArrayList<>();
        List<Object> promptStructure = new ArrayList<>();
        promptStructure.add(prompt);
        if (files != null && !files.isEmpty()) {
            promptStructure.add(0);
            promptStructure.add(null);
            List<List<Object>> fileDataList = new ArrayList<>();
            for (File file : files) {
                String uploadedFileId = uploadFile(file);
                List<Object> fileInfo = new ArrayList<>();
                fileInfo.add(Collections.singletonList(uploadedFileId));
                fileInfo.add(file.getName());
                fileDataList.add(fileInfo);
            }
            promptStructure.add(fileDataList);
        } else {
            promptStructure.addAll(Arrays.asList(0, null, null, null, null, 0));
        }
        promptAndFilesContainer.add(promptStructure);
        List<Object> innerList = new ArrayList<>();
        innerList.add(promptAndFilesContainer);
        innerList.add(Collections.singletonList("en"));
        List<Object> sessionPart = new ArrayList<>();
        if (chatSession != null && chatSession.getCid() != null) {
            sessionPart.add(chatSession.getCid());
            sessionPart.add(chatSession.getRid());
            sessionPart.add(chatSession.getRcid());
        } else {
            sessionPart.add("c_5955d70a75cfa3b8");
            sessionPart.add("r_e329d293f2d5d808");
            sessionPart.add("rc_f2820fbb865f20d3");
        }
        sessionPart.addAll(Arrays.asList(null, null, null, null, null, null,
                "AwAAAAAAAAAQ4DUFz3Xz2P4_hhYEiRk",
                "!NDelN2_NAAY1YgIEFWJCwmP7y0mwfSs7ADQBEArZ1O65RXl0xeGN06tf8JIhKbYE3p200ft8c-2-FFwCJbS1Ss7qGnsTR3xPoaJtiN07AgAAAltSAAABhWgBB34AQcTK706ky3rbtTY0GrjDKKarg66sj4ZPqQxxywKX5CvwAUrIYdAUIJ5ydkQgeVNmFzJnliWbrVjUSRwO86LsS-p-mQNyiZTOA1oYygi24GkAOxolnTjIzUIw28s3mpekbvzo-TPQW-vscxdIS2dmkpOmki7S4kcRY0wf7C_p2xW_zanGNU1OIZjj4Xwwta5xaLJ9YFEJzi-ibI2_M4cz1ZvXEyOG6OHg-I0LfKK8AyseIttD6Ga4p_31G6JuyQc3nq5v7x13fZEw-N31V6LZC6rEPfgrF1JFxhWsHBts2XPMmfP3n2AzrWZIdnQD02hjU2On5RIPv0A8cwPkZAlQY_-r08J01pnioINYOLG2w6dbaKMZcjZa_IEGfYpgewr30Kk_RYJ36hEaAu99dPZvDEOZ-b5YVCwrtBG2Eh2_XEHOBjnMhBcCO1Re2CCFP1UjtAZpJ7eWaAW_IpzdI09zMXf0B6MAmrEcdTtC11h6bgBOzahEAQmV5nTTDFEEYJpH1m8gQzHovQLgzVEPCfmB9ZSayWL88B_Wzprh6UNJLwSSuLNE39U4PsVDTVBq7chqp3HgowZ433WqGMJYELaCetTOATQa6vdQwe0kRAAA6A9TBVWho3Va_5Ah_uYD0d_LvDC-x7K7x9JBud13PeWP21r_J0tl92bmIIQuWJH83A6IDxbjrTb6gDxCrQGrAnTJXpYttAV--vwVgg0d7Itp0kzyJOGCH0tF2VSrYVu_Zj1mQ4fio1vmNJ2ZlhlQzrs1f9wfD6st6cnDKndT4ctEv4qKTrrCWzUci1yvRQnQv1eQqd4mugXgtqUtn2xsUoh_6Mmd_8tzXPluOnqITwax56H-uechYASGmvtWuiCDx0ZjOhGuA6hOas8vu2Q4vYK2x6xM3xOKh42FSErVjoBWSB7Uj2iMKlk1qFVMMUYU0rQgED1dfIVcL3ZKn29-Zh2rX1rXoxU07Vkh-p-zgzlYEOcww2NF2OeUBAMy4AtyNzebKWXLGYxocwfv_CpJr2-RAZSzhGzGu5uKj67rugTwd0N236q-SOrA_o8DvkXpq2_1GDgmJXHujbghqr3JiKhnbObbhMYibIKRHl8i9fdQqcGeOKc0Ph7hUI0wu8r0ZqWLfY5pUHAh5cA0W1ljzGrR07qDUQH3RoO-7JTaPDKAlE2l7EG2fOHPRkkprpy5KhkDE1vM1jBXprQ21kGz6Qk_GDGReuEhrn98PoifK2EZ49u84G6PcQfbxHs8il3H6UgGECYdZKkb",
                "8675212d13a9a6991c7a38d794f28bca"
        ));
        innerList.add(sessionPart);
        innerList.addAll(Arrays.asList(null, Collections.singletonList(0), 1, null, null, 1, 0, null, null, null, null, null,
                Collections.singletonList(Collections.singletonList(5)), 0, null, null, null, null, null, null, null, null,
                1, null, null, Collections.singletonList(4), null, null, null, null, null, null, null, null, null, null,
                Collections.singletonList(1), null, null, null, null, null, null, null, null, null, null, null, 0,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                Collections.emptyList()));
        String innerJsonString = gson.toJson(innerList);
        List<Object> outerList = new ArrayList<>();
        outerList.add(null);
        outerList.add(innerJsonString);
        return gson.toJson(outerList);
    }

    private int findBodyIndex(JsonArray responseJson) {
        for (int i = 0; i < responseJson.size(); i++) {
            try {
                JsonArray part = responseJson.get(i).getAsJsonArray();
                if (part.size() > 2 && !part.get(2).isJsonNull()) {
                    String mainPartJson = part.get(2).getAsString();
                    JsonArray mainPart = JsonParser.parseString(mainPartJson).getAsJsonArray();
                    if (mainPart.size() > 4 && !mainPart.get(4).isJsonNull() && !mainPart.get(4).getAsJsonArray().isEmpty()) {
                        return i;
                    }
                }
            } catch (Exception e) { /* Ignore */ }
        }
        return -1;
    }

    private JsonArray findImageBody(JsonArray responseJson, int start_index, int candidate_index) {
        for (int i = start_index; i < responseJson.size(); i++) {
            try {
                JsonArray part = responseJson.get(i).getAsJsonArray();
                if (part.size() > 2 && !part.get(2).isJsonNull()) {
                    String mainPartJson = part.get(2).getAsString();
                    JsonArray mainPart = JsonParser.parseString(mainPartJson).getAsJsonArray();
                    if (mainPart.size() > 4 && !mainPart.get(4).isJsonNull() && mainPart.get(4).getAsJsonArray().size() > candidate_index) {
                        JsonElement imgCandidateElement = mainPart.get(4).getAsJsonArray().get(candidate_index);
                        if (!imgCandidateElement.isJsonNull() && imgCandidateElement.getAsJsonArray().size() > 12) {
                            JsonElement c12_img = imgCandidateElement.getAsJsonArray().get(12);
                            if (!c12_img.isJsonNull() && c12_img.getAsJsonArray().size() > 7 && !c12_img.getAsJsonArray().get(7).isJsonNull() && !c12_img.getAsJsonArray().get(7).getAsJsonArray().isEmpty()) {
                                return mainPart;
                            }
                        }
                    }
                }
            } catch (Exception e) { /* Ignore */ }
        }
        return null;
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
