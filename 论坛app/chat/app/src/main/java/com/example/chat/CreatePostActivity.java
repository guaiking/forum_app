package com.example.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etPostTitle, etPostContent;
    private Button btnSubmitPost;
    private OkHttpClient client;
    private String userId;
    private static final String SERVER_URL = "http://192.168.43.143:3000/api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        etPostTitle = findViewById(R.id.etPostTitle);
        etPostContent = findViewById(R.id.etPostContent);
        btnSubmitPost = findViewById(R.id.btnSubmitPost);
        client = new OkHttpClient();

        // 获取用户ID
        userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_id", "");

        btnSubmitPost.setOnClickListener(v -> submitPost());
    }

    private void submitPost() {
        String title = etPostTitle.getText().toString().trim();
        String content = etPostContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "标题和内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody formBody = new FormBody.Builder()
                .add("userId", userId)
                .add("title", title)
                .add("content", content)
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL + "/create_post")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(CreatePostActivity.this, "发布失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        String status = jsonResponse.getString("status");
                        String message = jsonResponse.getString("message");

                        runOnUiThread(() -> {
                            if (status.equals("success")) {
                                Toast.makeText(CreatePostActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(CreatePostActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}