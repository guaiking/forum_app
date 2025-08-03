package com.example.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView ivAvatar;
    private TextView tvUsername;
    private EditText etNickname;
    private Button btnChangeAvatar, btnSaveProfile;
    private RecyclerView rvMyPosts;
    private MyPostAdapter myPostAdapter;
    private List<JSONObject> myPostList = new ArrayList<>();
    private OkHttpClient client;
    private String userId;
    private String currentAvatarPath = "";
    private static final String SERVER_URL = "http://192.168.43.143:3000/api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ivAvatar = findViewById(R.id.ivAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        etNickname = findViewById(R.id.etNickname);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        rvMyPosts = findViewById(R.id.rvMyPosts);
        client = new OkHttpClient();

        // 获取用户ID
        userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_id", "");

        rvMyPosts.setLayoutManager(new LinearLayoutManager(this));
        myPostAdapter = new MyPostAdapter();
        rvMyPosts.setAdapter(myPostAdapter);

        btnChangeAvatar.setOnClickListener(v -> openImageChooser());
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        loadProfile();
        loadMyPosts();
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            currentAvatarPath = imageUri.toString();
            Picasso.get().load(imageUri).into(ivAvatar);
        }
    }

    private void saveProfile() {
        String nickname = etNickname.getText().toString().trim();

        if (currentAvatarPath.isEmpty()) {
            // 只更新昵称
            updateProfile(nickname, "");
        } else {
            // 上传图片并更新资料
            uploadImageAndUpdateProfile(nickname);
        }
    }

    private void uploadImageAndUpdateProfile(String nickname) {
        try {
            Uri uri = Uri.parse(currentAvatarPath);
            File file = new File(uri.getPath());


            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", file.getName(),
                            RequestBody.create(MediaType.parse("image/*"), file))
                    .build();

            Request request = new Request.Builder()
                    .url(SERVER_URL + "/upload_avatar")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(ProfileActivity.this, "头像上传失败", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            String avatarUrl = jsonResponse.getString("avatarUrl");

                            updateProfile(nickname, avatarUrl);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "图片选择错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProfile(String nickname, String avatarUrl) {
        RequestBody formBody = new FormBody.Builder()
                .add("userId", userId)
                .add("nickname", nickname)
                .add("avatar", avatarUrl)
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL + "/update_profile")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this, "资料保存成功", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadProfile() {
        Request request = new Request.Builder()
                .url(SERVER_URL + "/profile/" + userId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this, "加载资料失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONObject profile = jsonResponse.getJSONObject("profile");

                        runOnUiThread(() -> {
                            try {
                                tvUsername.setText(profile.getString("username"));
                                etNickname.setText(profile.getString("nickname"));

                                if (!profile.getString("avatar").isEmpty()) {
                                    Picasso.get().load(profile.getString("avatar")).into(ivAvatar);
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

    private void loadMyPosts() {
        Request request = new Request.Builder()
                .url(SERVER_URL + "/user_posts/" + userId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this, "加载帖子失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONArray postsArray = jsonResponse.getJSONArray("posts");

                        myPostList.clear();
                        for (int i = 0; i < postsArray.length(); i++) {
                            myPostList.add(postsArray.getJSONObject(i));
                        }

                        runOnUiThread(() -> myPostAdapter.notifyDataSetChanged());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private class MyPostAdapter extends RecyclerView.Adapter<MyPostAdapter.MyPostViewHolder> {

        @Override
        public MyPostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new MyPostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyPostViewHolder holder, int position) {
            try {
                JSONObject post = myPostList.get(position);
                holder.tvTitle.setText(post.getString("title"));

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(ProfileActivity.this, PostDetailActivity.class);
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
            return myPostList.size();
        }

        class MyPostViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;

            MyPostViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}