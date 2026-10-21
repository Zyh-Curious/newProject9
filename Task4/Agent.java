package org.example;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import okio.BufferedSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Agent {
    private static final String API_KEY = "";
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL = "deepseek-chat";


    private static final Pattern DANGEROUS_COMMANDS = Pattern.compile(
            "(?i).*\\b(rm|rmdir|del|format|mkfs|dd|shutdown|reboot|:)\\b.*"
    );

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    private final JSONArray messages = new JSONArray();
    private final Scanner scanner = new Scanner(System.in);

    public Agent() {
        String systemPrompt = loadPrompt();
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);
    }

    private String loadPrompt() {
        try {
            return Files.readString(Paths.get("./ystem_prompt.txt"));
        } catch (IOException e) {
            System.err.println("未找到 system_prompt.txt，使用默认 Prompt");
            return "你是一个 AI 助手。调用工具请输出 JSON: {\"tool\": \"execute_command\", \"command\": \"命令\"}";
        }
    }


    private String chatWithLLMStream() throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.1);
        requestBody.put("stream", true);

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toJSONString(), MediaType.get("application/json")))
                .build();

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    if (response.code() >= 500 || response.code() == 429) {
                        throw new IOException("API 错误码: " + response.code());
                    }
                    throw new IOException("请求失败: " + response.code() + " " + response.message());
                }

                ResponseBody body = response.body();
                if (body == null) throw new IOException("Response body is null");

                StringBuilder fullResponse = new StringBuilder();
                BufferedSource source = body.source();
                String line;

                System.out.print("Agent: ");

                while ((line = source.readUtf8Line()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;

                        try {
                            JSONObject chunk = JSON.parseObject(data);
                            JSONArray choices = chunk.getJSONArray("choices");
                            if (choices != null && !choices.isEmpty()) {
                                JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                                if (delta != null && delta.containsKey("content")) {
                                    String content = delta.getString("content");
                                    System.out.print(content);
                                    System.out.flush();
                                    fullResponse.append(content);
                                }
                            }
                        } catch (Exception e) {

                        }
                    }
                }
                System.out.println();
                return fullResponse.toString();

            } catch (IOException e) {

                System.err.println("\n网络或 API 异常: " + e.getMessage());
                if (attempt == maxRetries) {
                    throw new IOException("重试 " + maxRetries + " 次后仍然失败，终止。", e);
                }
                int waitSeconds = attempt * 2; // 指数退避
                System.err.println("第 " + attempt + " 次重试，等待 " + waitSeconds + " 秒...");
                try {
                    Thread.sleep(waitSeconds * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return "";
    }


    private String executeCommand(String command) {

        if (DANGEROUS_COMMANDS.matcher(command).matches()) {
            System.out.println("\n[安全警告] AI 试图执行危险命令: " + command);
            System.out.print("是否允许执行？(y/n): ");
            String answer = scanner.nextLine().trim().toLowerCase();
            if (!"y".equals(answer)) {
                System.out.println("用户拒绝了该命令。");
                return "操作被用户拒绝：这是一个可能破坏系统的危险命令，请停止尝试。";
            }
        }

        System.out.println("[执行命令]: " + command);
        StringBuilder output = new StringBuilder();
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb = os.contains("win")
                    ? new ProcessBuilder("cmd.exe", "/c", command)
                    : new ProcessBuilder("bash", "-c", command);

            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            if (output.length() == 0) {
                output.append("执行完成，无输出 (Exit Code: ").append(exitCode).append(")");
            }

            if (output.length() > 3000) return output.substring(0, 3000) + "\n...[截断]";
            return output.toString();
        } catch (Exception e) {
            return "执行异常: " + e.getMessage();
        }
    }


    public void run() {
        System.out.println(" Agent 已启动！输入 'exit' 退出。");

        while (true) {
            System.out.print("\n你: ");
            if (!scanner.hasNextLine()) break;
            String userInput = scanner.nextLine().trim();
            if (userInput.isEmpty()) continue;
            if ("exit".equalsIgnoreCase(userInput)) break;

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userInput);
            messages.add(userMsg);

            int maxIterations = 5;
            for (int i = 0; i < maxIterations; i++) {
                try {
                    String llmResponse = chatWithLLMStream();


                    if (llmResponse.contains("\"tool\":") && llmResponse.contains("execute_command")) {
                        String jsonStr = llmResponse;
                        if (jsonStr.contains("{")) {
                            jsonStr = jsonStr.substring(jsonStr.indexOf("{"), jsonStr.lastIndexOf("}") + 1);
                        }
                        JSONObject toolCall = JSON.parseObject(jsonStr.trim());
                        String command = toolCall.getString("command");

                        String toolResult = executeCommand(command);

                        JSONObject assistantMsg = new JSONObject();
                        assistantMsg.put("role", "assistant");
                        assistantMsg.put("content", llmResponse);
                        messages.add(assistantMsg);

                        JSONObject toolResultMsg = new JSONObject();
                        toolResultMsg.put("role", "user");
                        toolResultMsg.put("content", "终端执行结果:\n" + toolResult);
                        messages.add(toolResultMsg);
                    } else {
                        JSONObject assistantMsg = new JSONObject();
                        assistantMsg.put("role", "assistant");
                        assistantMsg.put("content", llmResponse);
                        messages.add(assistantMsg);
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("核心错误: " + e.getMessage());
                    break;
                }
            }
        }
        scanner.close();
    }

    public static void main(String[] args) {
        new Agent().run();
    }
}
