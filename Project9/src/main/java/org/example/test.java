package org.example;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class test {
    public static void main(String[] args) throws IOException, InterruptedException {
        HttpClient c =  HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://test.glimmer.org.cn/echo/"))
                .GET()
                .header("user_name", "123")
                .build();
        HttpResponse<String> response = c.send(request,HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        System.out.println(response.statusCode());
    }
}
