package com.example.chat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnGoToRegister;
    private TextView tvMessage;
    private OkHttpClient client;
    private static final String SERVER_URL = "http://192.168.43.143:3000/api"; // 替换为你的服务器地址

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        tvMessage = findViewById(R.id.tvMessage);
        client = new OkHttpClient();

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            tvMessage.setText("用户名和密码不能为空");
            return;
        }

        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL + "/login")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvMessage.setText("网络错误: " + e.getMessage()));
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
                            // 在登录成功的回调中添加保存用户ID
                            if (status.equals("success")) {
                                tvMessage.setText("");
                                Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();

                                JSONObject userObj = null;
                                try {
                                    userObj = jsonResponse.getJSONObject("user");
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                SharedPreferences.Editor editor = getSharedPreferences("user_prefs", MODE_PRIVATE).edit();
                                try {
                                    editor.putString("user_id", userObj.getString("id"));
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                try {
                                    editor.putString("username", userObj.getString("username"));
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                try {
                                    editor.putString("nickname", userObj.optString("nickname", userObj.getString("username"))); // 默认昵称=用户名
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                editor.apply();


                                // 登录成功后跳转到主页面
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                tvMessage.setText(message);
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
