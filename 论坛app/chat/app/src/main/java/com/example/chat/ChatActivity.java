package com.example.chat;


import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String API_KEY = ""; // 替换为你的API密钥
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String SERVER_URL = "http://192.168.43.143:3000/api";

    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend;
    private ChatAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private ProgressDialog progressDialog;
    private OkHttpClient client;
    private Gson gson = new Gson();
    private DrawerLayout drawerLayout;
    private String currentConversationId;
    private String userId; // 从登录获取的用户ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 获取用户ID (需要在登录时保存并传递)
        userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_id", "");

        // 初始化HTTP客户端
        client = createHttpClient();
        side();

        // 初始化视图
        initViews();

        // 设置聊天列表
        setupRecyclerView();

        // 加载对话列表
        loadConversations();

        // 如果没有当前对话，添加欢迎消息
        if (currentConversationId == null) {
            addWelcomeMessage();
        }
        ImageButton btnNewChat = findViewById(R.id.new_chat);
        btnNewChat.setOnClickListener(v -> {
            // 清空当前对话
            messages.clear();
            adapter.notifyDataSetChanged();
            currentConversationId = null;
            addWelcomeMessage();
        });
    }

    // 修改 side() 方法
    private void side() {
        drawerLayout = findViewById(R.id.drawer_layout);
        LinearLayout drawerMenu = findViewById(R.id.drawer_menu); // 需要给侧边栏LinearLayout添加id

        // 点击按钮打开侧边栏
        ImageButton btnOpenDrawer = findViewById(R.id.three);
        btnOpenDrawer.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });
    }

    // 添加加载对话列表的方法
    private void loadConversations() {
        if (userId.isEmpty()) return;

        Request request = new Request.Builder()
                .url(SERVER_URL + "/conversations/" + userId)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "加载对话列表失败", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);

                        if (json.getString("status").equals("success")) {
                            JSONArray conversations = json.getJSONArray("conversations");

                            runOnUiThread(() -> {
                                // 清空侧边栏菜单
                                LinearLayout drawerMenu = findViewById(R.id.drawer_menu);
                                drawerMenu.removeAllViews();

                                // 添加标题
                                TextView title = new TextView(ChatActivity.this);
                                title.setText("历史对话");
                                title.setTextSize(18);
                                title.setPadding(16, 16, 16, 16);
                                drawerMenu.addView(title);

                                // 添加分割线
                                View divider = new View(ChatActivity.this);
                                divider.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                                divider.setBackgroundColor(Color.GRAY);
                                drawerMenu.addView(divider);

                                for (int i = 0; i < conversations.length(); i++) {
                                    JSONObject conv = null;
                                    try {
                                        conv = conversations.getJSONObject(i);
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                    final String id; // 声明为final
                                    try {
                                        id = conv.getString("id");
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                    String title1 = null;
                                    try {
                                        title1 = conv.getString("title");
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }

                                    TextView convItem = new TextView(ChatActivity.this);
                                    convItem.setText(title1);
                                    convItem.setPadding(16, 12, 16, 12);
                                    convItem.setTextSize(16);
                                    convItem.setOnClickListener(v -> loadConversation(id)); // 现在可以使用id

                                    drawerMenu.addView(convItem);

                                    // 添加分割线
                                    View itemDivider = new View(ChatActivity.this);
                                    itemDivider.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT, 1));
                                    itemDivider.setBackgroundColor(Color.LTGRAY);
                                    drawerMenu.addView(itemDivider);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析对话列表失败", e);
                    }
                }
            }
        });
    }

    // 添加加载单个对话的方法
    private void loadConversation(String id) {
        Request request = new Request.Builder()
                .url(SERVER_URL + "/conversation/" + id)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "加载对话失败", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);

                        if (json.getString("status").equals("success")) {
                            String content = json.getString("content");
                            JSONArray messagesArray = new JSONArray(content);

                            runOnUiThread(() -> {
                                messages.clear();
                                for (int i = 0; i < messagesArray.length(); i++) {
                                    JSONObject msg = null;
                                    try {
                                        msg = messagesArray.getJSONObject(i);
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                    try {
                                        messages.add(new ChatMessage(
                                                msg.getString("content"),
                                                msg.getBoolean("isUser")
                                        ));
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                adapter.notifyDataSetChanged();
                                recyclerView.scrollToPosition(messages.size() - 1);
                                drawerLayout.closeDrawer(GravityCompat.START);

                                // 设置当前对话ID
                                currentConversationId = id;
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析对话内容失败", e);
                    }
                }
            }
        });
    }


    @Override
    public void onBackPressed() {
        // 按返回键时优先关闭侧边栏
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private OkHttpClient createHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .build();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                recyclerView.postDelayed(() -> {
                    if (messages.size() > 0) {
                        recyclerView.smoothScrollToPosition(messages.size() - 1);
                    }
                }, 100);
            }
        });
    }

    private void addWelcomeMessage() {
        addMessage(new ChatMessage("你好！我是DeepSeek助手，有什么可以帮你的吗？", false));
    }

    // 修改 sendMessage 方法，在调用API成功后保存对话
    private void sendMessage() {
        String messageContent = etMessage.getText().toString().trim();
        if (messageContent.isEmpty()) {
            return;
        }

        // 添加用户消息到列表
        ChatMessage userMessage = new ChatMessage(messageContent, true);
        addMessage(userMessage);
        etMessage.setText("");

        // 调用DeepSeek API
        callDeepSeekAPI(messageContent);
    }

    private void callDeepSeekAPI(String message) {
        if (!isNetworkAvailable()) {
            showToast("网络不可用，请检查连接");
            addMessage(new ChatMessage("无法连接到网络", false));
            return;
        }

        showProgressDialog("正在获取回复...");

        try {
            DeepSeekRequest request = new DeepSeekRequest();
            request.model = "deepseek-chat";
            request.messages = createMessagesWithContext(message);
            request.temperature = 0.7;
            request.max_tokens = 1000;

            String json = gson.toJson(request);
            Log.d(TAG, "API Request: " + json);

            RequestBody body = RequestBody.create(json, JSON);
            Request apiRequest = new Request.Builder()
                    .url(DEEPSEEK_API_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(apiRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API Call Failed", e);
                    runOnUiThread(() -> {
                        dismissProgressDialog();
                        if (e instanceof SocketTimeoutException) {
                            handleTimeoutError(message);
                        } else {
                            addMessage(new ChatMessage("网络错误: " + e.getMessage(), false));
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "API Response: " + responseBody);

                        if (!response.isSuccessful()) {
                            handleErrorResponse(response.code(), responseBody);
                            return;
                        }

                        DeepSeekResponse apiResponse = gson.fromJson(responseBody, DeepSeekResponse.class);
                        processAPIResponse(apiResponse);

                    } catch (Exception e) {
                        Log.e(TAG, "Response Processing Error", e);
                        runOnUiThread(() -> {
                            dismissProgressDialog();
                            addMessage(new ChatMessage("处理回复时出错: " + e.getMessage(), false));
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Request Construction Error", e);
            dismissProgressDialog();
            addMessage(new ChatMessage("创建请求失败: " + e.getMessage(), false));
        }
    }

    private DeepSeekRequest.Message[] createMessagesWithContext(String newMessage) {
        List<ChatMessage> recentMessages = getRecentMessages(2);
        DeepSeekRequest.Message[] messages = new DeepSeekRequest.Message[recentMessages.size() + 1];

        for (int i = 0; i < recentMessages.size(); i++) {
            ChatMessage msg = recentMessages.get(i);
            DeepSeekRequest.Message apiMsg = new DeepSeekRequest.Message();
            apiMsg.role = msg.isUser() ? "user" : "assistant";
            apiMsg.content = msg.getContent();
            messages[i] = apiMsg;
        }

        DeepSeekRequest.Message currentMsg = new DeepSeekRequest.Message();
        currentMsg.role = "user";
        currentMsg.content = newMessage;
        messages[messages.length - 1] = currentMsg;

        return messages;
    }

    private List<ChatMessage> getRecentMessages(int count) {
        int start = Math.max(0, messages.size() - count);
        return new ArrayList<>(messages.subList(start, messages.size()));
    }

    private void handleTimeoutError(String message) {
        addMessage(new ChatMessage("请求超时，正在重试...", false));
        // 延迟1秒后重试
        recyclerView.postDelayed(() -> callDeepSeekAPI(message), 1000);
    }

    private void handleErrorResponse(int code, String body) {
        runOnUiThread(() -> {
            dismissProgressDialog();
            try {
                JSONObject errorJson = new JSONObject(body);
                String errorMsg = errorJson.optString("message", "未知错误");
                addMessage(new ChatMessage("API错误(" + code + "): " + errorMsg, false));
            } catch (JSONException e) {
                addMessage(new ChatMessage("服务错误: HTTP " + code, false));
            }
        });
    }

    // 修改 processAPIResponse 方法，在获取回复后保存对话
    private void processAPIResponse(DeepSeekResponse response) {
        runOnUiThread(() -> {
            dismissProgressDialog();
            if (response.error != null) {
                addMessage(new ChatMessage("API返回错误: " + response.error, false));
                return;
            }

            if (response.choices == null || response.choices.length == 0) {
                addMessage(new ChatMessage("API返回空响应", false));
                return;
            }

            String rawReply = response.choices[0].message.content;
            String markdownReply = MarkdownConverter.toMarkdown(rawReply);
            addMessage(new ChatMessage(markdownReply, false));

            // 保存/更新对话
            saveConversation();
        });
    }

    // 添加保存对话的方法
    private void saveConversation() {
        if (userId.isEmpty()) return;

        // 生成对话标题 (取前几条消息的前几个字)
        String title = generateConversationTitle();

        // 将消息列表转为JSON
        String content = gson.toJson(messages);

        // 构建请求体
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("userId", userId);
            requestBody.put("title", title);
            requestBody.put("content", content);
            // 如果存在当前对话ID，则传入用于更新
            if (currentConversationId != null) {
                requestBody.put("conversationId", currentConversationId);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(requestBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(SERVER_URL + "/save_conversation")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "保存对话失败", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);

                        if (json.getString("status").equals("success")) {
                            // 如果是新创建的对话，保存ID
                            if (currentConversationId == null) {
                                currentConversationId = json.getString("conversationId");
                            }
                            // 刷新对话列表
                            loadConversations();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析保存对话响应失败", e);
                    }
                }
            }
        });
    }


    // 生成对话标题
    private String generateConversationTitle() {
        if (messages.isEmpty()) return "新对话";

        StringBuilder titleBuilder = new StringBuilder();
        int count = 0;

        for (ChatMessage message : messages) {
            if (count >= 3) break; // 最多取前3条消息
            if (!message.isUser()) continue; // 只取用户消息

            String content = message.getContent();
            if (content.length() > 15) {
                content = content.substring(0, 15) + "...";
            }

            if (titleBuilder.length() > 0) {
                titleBuilder.append(" / ");
            }
            titleBuilder.append(content);
            count++;
        }

        return titleBuilder.length() > 0 ? titleBuilder.toString() : "新对话";
    }

    private void addMessage(ChatMessage message) {
        runOnUiThread(() -> {
            messages.add(message);
            adapter.notifyItemInserted(messages.size() - 1);
            recyclerView.smoothScrollToPosition(messages.size() - 1);
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void showProgressDialog(String message) {
        runOnUiThread(() -> {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(ChatActivity.this);
                progressDialog.setCancelable(false);
            }
            progressDialog.setMessage(message);
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
        });
    }

    private void dismissProgressDialog() {
        runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(ChatActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    // DeepSeek API请求数据结构
    private static class DeepSeekRequest {
        String model;
        Message[] messages;
        double temperature;
        int max_tokens;

        static class Message {
            String role;
            String content;
        }
    }

    // DeepSeek API响应数据结构
    private static class DeepSeekResponse {
        String id;
        String object;
        long created;
        String model;
        Choice[] choices;
        Usage usage;
        String error;

        static class Choice {
            int index;
            Message message;
            String finish_reason;

            static class Message {
                String role;
                String content;
            }
        }

        static class Usage {
            int prompt_tokens;
            int completion_tokens;
            int total_tokens;
        }
    }
}
