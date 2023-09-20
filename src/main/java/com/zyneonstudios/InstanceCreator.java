package com.zyneonstudios;

import com.zyneonstudios.instance.creator.FileChooser;
import com.zyneonstudios.instance.creator.JCefFrame;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class InstanceCreator {

    private static FileChooser fileChooser;

    public static void main(String[] args) {
        fileChooser = new FileChooser();
        JCefFrame frame;
        try {
            frame = new JCefFrame();
        } catch (UnsupportedPlatformException | InterruptedException | IOException | CefInitializationException e) {
            throw new RuntimeException(e);
        }
        frame.open();
    }

    public static FileChooser getFileChooser() {
        return fileChooser;
    }

    public static boolean upload(String token, String sourceFilePath, String fileName) {
        try {
            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(0, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).writeTimeout(0, TimeUnit.SECONDS).build();
            String fileContent = Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(sourceFilePath)));
            String apiUrl = "https://api.github.com/repos/danieldieeins/ZyneonApplicationContent/contents/m/" + fileName;
            Request checkRequest = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
            Response checkResponse = client.newCall(checkRequest).execute();
            String currentSha = "";
            if (checkResponse.isSuccessful()) {
                JSONObject jsonResponse = new JSONObject(checkResponse.body().string());
                currentSha = jsonResponse.getString("sha");
            }
            JSONObject json = new JSONObject();
            json.put("message", "Automated upload via Zyneon Instance Creator");
            json.put("content", fileContent);
            if (!currentSha.isEmpty()) {
                json.put("sha", currentSha);
            }
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, json.toString());
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .method("PUT", body)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
            Response response = client.newCall(request).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}