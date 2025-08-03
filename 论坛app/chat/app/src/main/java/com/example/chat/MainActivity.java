package com.example.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvPosts;
    private MainActivity.PostAdapter postAdapter;
    private List<JSONObject> postList = new ArrayList<>();
    private OkHttpClient client;
    private static final String SERVER_URL = "http://192.168.43.143:3000/api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 悬浮按钮的拖动和点击功能
        ImageButton floatingBtn = findViewById(R.id.floating_btn);
        floatingBtn.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private long downTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        downTime = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        v.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        break;
                    case MotionEvent.ACTION_UP:
                        // 判断是否为点击（短时间且移动距离小）
                        if (System.currentTimeMillis() - downTime < 200 &&
                                Math.abs(event.getRawX() + dX - v.getX()) < 10 &&
                                Math.abs(event.getRawY() + dY - v.getY()) < 10) {
                            openChatActivity();
                        }
                        break;
                }
                return true;
            }
        });

        // 右上角添加按钮点击事件
        ImageButton addButton = findViewById(R.id.imageButton);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreatePostActivity.class);
            startActivity(intent);
        });

        // 论坛按钮点击事件
        ImageButton forumButton = findViewById(R.id.imageButton3);

        // 个人按钮点击事件
        ImageButton profileButton = findViewById(R.id.imageButton4);
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        rvPosts = findViewById(R.id.recyclerView);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new MainActivity.PostAdapter();
        rvPosts.setAdapter(postAdapter);
        client = new OkHttpClient();


    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();  // 每次返回 MainActivity 时刷新数据
    }

    private void openChatActivity() {
        Intent intent = new Intent(this, ChatActivity.class);
        startActivity(intent);
    }
    private void loadPosts() {
        Request request = new Request.Builder()
                .url(SERVER_URL + "/posts")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONArray postsArray = jsonResponse.getJSONArray("posts");

                        postList.clear();
                        for (int i = 0; i < postsArray.length(); i++) {
                            postList.add(postsArray.getJSONObject(i));
                        }

                        runOnUiThread(() -> postAdapter.notifyDataSetChanged());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private class PostAdapter extends RecyclerView.Adapter<MainActivity.PostAdapter.PostViewHolder> {

        @Override
        public MainActivity.PostAdapter.PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_post, parent, false);
            return new MainActivity.PostAdapter.PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MainActivity.PostAdapter.PostViewHolder holder, int position) {
            try {
                JSONObject post = postList.get(position);

                String nickname = post.has("nickname") && !post.isNull("nickname") ?
                        post.getString("nickname") : post.getString("username");

                holder.tvNickname.setText(nickname);
                holder.tvTitle.setText(post.getString("title"));
                holder.tvTime.setText(post.getString("created_at"));

                if (post.has("avatar") && !post.isNull("avatar") && !post.getString("avatar").isEmpty()) {
                    Picasso.get().load(post.getString("avatar")).into(holder.ivAvatar);
                }

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, PostDetailActivity.class);
                    try {
                        intent.putExtra("postId", post.getString("id"));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    startActivity(intent);
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return postList.size();
        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvNickname, tvTitle, tvTime;

            PostViewHolder(View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvNickname = itemView.findViewById(R.id.tvNickname);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvTime = itemView.findViewById(R.id.tvTime);
            }
        }
    }
}