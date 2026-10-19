package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

public class SimpleDownloader {
    public static final String TEST_URL = "https://test.glimmer.org.cn/re/the_last_supper.png";
    private static final String SAVE_PATH = "the_last_supper.png";
    public static void main(String[] args) {
        // 创建一个HttpClient
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS).build()) {
            // 下载计时
            LocalDateTime startTime = LocalDateTime.now();

            // TODO 构建request并发送这个request，需要添加header：“Authorization: Bearer glimmer_recruit”，否则返回401错误。你的代码应写在TODO和END TODO之间。
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TEST_URL))
                    .GET()
                    .header("Authorization","Bearer glimmer_recruit")
                    .build();
            HttpResponse<Path> response = client.send(request,HttpResponse.BodyHandlers.ofFile(Path.of(SAVE_PATH)));



            // END TODO
            // 结束计时
            System.out.println("耗时：" + ((double) Duration.between(startTime, LocalDateTime.now()).toMillis() / 1000) + "s");
        } catch (IOException e) {
            System.err.println("出现错误！请检查你的网络连通性，或者咨询出题人服务器是否正常。");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("下载操作被中断！");
            e.printStackTrace();
        }
    }
}
