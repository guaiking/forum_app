package com.example.chat;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.squareup.picasso.Picasso;
import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class PostDetailActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private TextView tvNickname, tvTitle, tvTime, tvContent;
    private OkHttpClient client;
    private static final String SERVER_URL = "http://192.168.43.143:3000/api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        ivAvatar = findViewById(R.id.ivAvatar);
        tvNickname = findViewById(R.id.tvNickname);
        tvTitle = findViewById(R.id.tvTitle);
        tvTime = findViewById(R.id.tvTime);
        tvContent = findViewById(R.id.tvContent);
        client = new OkHttpClient();

        String postId = getIntent().getStringExtra("postId");
        loadPostDetail(postId);
    }

    private void loadPostDetail(String postId) {
        Request request = new Request.Builder()
                .url(SERVER_URL + "/post/" + postId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(PostDetailActivity.this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONObject post = jsonResponse.getJSONObject("post");

                        runOnUiThread(() -> {
                            try {
                                String nickname = post.has("nickname") && !post.isNull("nickname") ?
                                        post.getString("nickname") : post.getString("username");

                                tvNickname.setText(nickname);
                                tvTitle.setText(post.getString("title"));
                                tvTime.setText(post.getString("created_at"));
                                tvContent.setText(post.getString("content"));

                                if (post.has("avatar") && !post.isNull("avatar") && !post.getString("avatar").isEmpty()) {
                                    Picasso.get().load(post.getString("avatar")).into(ivAvatar);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
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