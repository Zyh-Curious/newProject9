package org.example;

import com.alibaba.fastjson2.JSON;
import okhttp3.*;

import java.io.IOException;



public class UserManager{
    public static final OkHttpClient  client1= new OkHttpClient.Builder().addInterceptor(new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request().newBuilder()
                    .addHeader("Authorization", auth)
                    .build();

            return chain.proceed(request);
        }
                                                                                         }
    ).build();

    private static final String host = "https://test.glimmer.org.cn";
    private static final String auth = "Bearer glimmer_recruit";
    public record User(long id, String nickname) {}

    // 新增用户
    public static boolean addUser(User user) {
        Request request = new Request.Builder()
                .url(host + "/users")
                .post(RequestBody.create(JSON.toJSONString(user), MediaType.get("application/json")))
                .build();
        return execute(request);
    }

    // 删除目标id对应的用户
    public static boolean removeUser(long id) {
        Request request = new Request.Builder()
                .url(host + "/users/" + id)
                .delete()
                .build();
        return execute(request);
    }

    // 更新目标用户的信息
    public static boolean updateUser(User user) {
        Request request = new Request.Builder()
                .url(host + "/users/" + user.id)
                .put(RequestBody.create(JSON.toJSONString(user), MediaType.get("application/json")))
                .build();
        return execute(request);
    }

    // 根据id查询对应的用户
    public static User getUser(long id) {
        Request request = new Request.Builder()
                .url(host + "/users/" + id)
                .get()
                .build();
        try (Response res = client1.newCall(request).execute()) {
            if (res.isSuccessful()) {
                ResponseBody body = res.body();
                if (body != null) return JSON.parseObject(body.string(), User.class);
            }
        } catch (IOException e) {
            System.err.println("请求出现异常，请检查网络可达性");
        }
        return null;
    }

    // 简单提取重复代码，用于发送请求，并返回是否成功
    private static boolean execute(Request request) {
        try (Response res = client1.newCall(request).execute()) {
            if (!res.isSuccessful()) {
                System.err.println("请求失败，状态码: " + res.code());
                // 如果是 401/403，可以看看服务端返回的 body 是什么
                if (res.body() != null) {
                    System.err.println("服务端返回: " + res.body().string());
                }
            }
            return res.isSuccessful();
        } catch (IOException e) {
            e.printStackTrace(); // 👈 关键：打印完整堆栈
            System.err.println("请求 URL: " + request.url());
            System.err.println("请求出现异常: " + e.getMessage());
        }
        return false;
    }
}
