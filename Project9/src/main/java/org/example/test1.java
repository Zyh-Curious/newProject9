package org.example;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class test1 {
    public static void main(String[] args) throws IOException, InterruptedException {
        Users u = new Users();
        u.setUsername("123");
        u.setPassword("123456");
        u.setEmail("123@qq.com");

        Gson gson = new Gson();
        String json = gson.toJson(u);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://test.glimmer.org.cn/echo/"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
        JsonObject responseJson = new JsonParser().parse(response.body()).getAsJsonObject();
        JsonObject msg = responseJson.get("body").getAsJsonObject();
        Gson printer = new GsonBuilder().setPrettyPrinting().create();

        System.out.println(printer.toJson(msg));


    }

}