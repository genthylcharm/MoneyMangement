package com.dvl.moneymanagement.Activity.Authentication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import com.dvl.moneymanagement.Activity.Base.BaseActivity;
import com.dvl.moneymanagement.DataBase.DatabaseHelper;
import com.dvl.moneymanagement.Activity.MainActivity;
import com.dvl.moneymanagement.Model.User;
import com.dvl.moneymanagement.Activity.Utils;
import com.dvl.moneymanagement.Activity.WebsiteActivity;
import com.dvl.moneymanagement.databinding.ActivityLoginBinding;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private static final String TAG = "LoginActivity";
    private Button btnLogin;
    private DatabaseHelper databaseHelper;
    private LoginUser loginUser;
    private DoesEmailExist doesEmailExist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.txtLicense.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, WebsiteActivity.class);
            startActivity(intent);
        });

        binding.txtRegister.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

       binding.btnLogin.setOnClickListener(view -> {
            hideKeyboard();
            initLogin();
        });
    }

    // Ẩn bàn phím khi nhấn vào nút đăng nhập
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // Xác thực trường nhập dữ liệu
    private void initLogin() {
        Log.d(TAG, "initLogin: started");

        String email = binding.edtTxtEmail.getText().toString().trim();
        String password = binding.edtTxtPassword.getText().toString().trim();

        if (!email.isEmpty()) {
            if (!password.isEmpty()) {
                binding.txtWarning.setVisibility(View.GONE);

                if (isValidEmail(email)) {
                    doesEmailExist = new DoesEmailExist();
                    doesEmailExist.execute(email);
                } else {
                    binding.txtWarning.setVisibility(View.VISIBLE);
                    binding.txtWarning.setText("Please enter a valid email address");
                }
            } else {
                binding.txtWarning.setVisibility(View.VISIBLE);
                binding.txtWarning.setText("Please enter your Password");
            }
        } else {
          binding.txtWarning.setVisibility(View.VISIBLE);
           binding.txtWarning.setText("Please enter your email address");
        }
    }

    // Kiểm tra định dạng email hợp lệ
    private boolean isValidEmail(CharSequence target) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    // Kiểm tra email có tồn tại không
    private class DoesEmailExist extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            databaseHelper = new DatabaseHelper(LoginActivity.this);
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            String email = strings[0];
            try (SQLiteDatabase db = databaseHelper.getReadableDatabase();
                 Cursor cursor = db.query("users", new String[]{"email"}, "email=?",
                         new String[]{email}, null, null, null)) {
                return cursor != null && cursor.moveToFirst();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean emailExists) {
            super.onPostExecute(emailExists);

            if (emailExists) {
                loginUser = new LoginUser();
                loginUser.execute();
            } else {
                binding.txtWarning.setVisibility(View.VISIBLE);
                binding.txtWarning.setText("There is no such user with this email address");
            }
        }
    }

    // Đăng nhập người dùng
    private class LoginUser extends AsyncTask<Void, Void, User> {
        private String email;
        private String password;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.email =binding.edtTxtEmail.getText().toString().trim();
            this.password = binding.edtTxtPassword.getText().toString().trim();
        }

        @SuppressLint("Range")
        @Override
        protected User doInBackground(Void... voids) {
            try (SQLiteDatabase db = databaseHelper.getReadableDatabase();
                 Cursor cursor = db.query("users", null, "email=? AND password=?",
                         new String[]{email, password}, null, null, null)) {

                if (cursor != null && cursor.moveToFirst()) {
                    User user = new User();
                    user.set_id(cursor.getInt(cursor.getColumnIndex("_id")));
                    user.setEmail(cursor.getString(cursor.getColumnIndex("email")));
                    user.setPassword(cursor.getString(cursor.getColumnIndex("password")));
                    user.setFirst_name(cursor.getString(cursor.getColumnIndex("first_name")));
                    user.setLast_name(cursor.getString(cursor.getColumnIndex("last_name")));
                    user.setImage_url(cursor.getString(cursor.getColumnIndex("image_url")));
                    user.setAddress(cursor.getString(cursor.getColumnIndex("address")));
                    user.setRemained_amount(cursor.getDouble(cursor.getColumnIndex("remained_amount")));
                    return user;
                } else {
                    return null;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(User user) {
            super.onPostExecute(user);

            if (user != null) {
                Utils utils = new Utils(LoginActivity.this);
                utils.addUserToSharedPreferences(user);

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                binding.txtWarning.setVisibility(View.VISIBLE);
                binding.txtWarning.setText("Your password is incorrect");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (doesEmailExist != null && !doesEmailExist.isCancelled()) {
            doesEmailExist.cancel(true);
        }

        if (loginUser != null && !loginUser.isCancelled()) {
            loginUser.cancel(true);
        }
    }
}
