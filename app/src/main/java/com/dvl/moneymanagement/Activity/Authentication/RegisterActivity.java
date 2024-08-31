package com.dvl.moneymanagement.Activity.Authentication;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.dvl.moneymanagement.Activity.Base.BaseActivity;
import com.dvl.moneymanagement.DataBase.DatabaseHelper;
import com.dvl.moneymanagement.Activity.MainActivity;
import com.dvl.moneymanagement.Model.User;
import com.dvl.moneymanagement.Activity.Utils;
import com.dvl.moneymanagement.Activity.WebsiteActivity;
import com.dvl.moneymanagement.databinding.ActivityRegisterBinding;

public class RegisterActivity extends BaseActivity {
    private static final String TAG = "RegisterActivity";
    private ActivityRegisterBinding binding;
    private String image_url;
    private DatabaseHelper databaseHelper;

    private DoesUserExist doesUserExistTask;
    private RegisterUser registerUserTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = new DatabaseHelper(this);
        image_url = "first"; // Set default image_url
        handleImageUrl();

        binding.btnRegister.setOnClickListener(view -> initRegister());

        binding.txtLogin.setOnClickListener(view -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));

        binding.txtLicense.setOnClickListener(view -> startActivity(new Intent(RegisterActivity.this, WebsiteActivity.class)));
    }

    private void handleImageUrl() {
        Log.d(TAG, "handleImageUrl: started");

        // Mảng chứa các hình ảnh và URL tương ứng
        ImageView[] images = {
                binding.firstImage,
                binding.secondImage,
                binding.thirdImage,
                binding.forthImage,
                binding.fifthImage
        };
        String[] urls = {"first", "second", "third", "forth", "fifth"};

        // Vòng lặp để thiết lập OnClickListener cho từng hình ảnh
        for (int i = 0; i < images.length; i++) {
            final String url = urls[i];
            images[i].setOnClickListener(view -> image_url = url);
        }
    }

    private void initRegister() {
        Log.d(TAG, "initRegister: started");

        String email = binding.edtTxtEmail.getText().toString().trim();
        String password = binding.edtTxtPassword.getText().toString().trim();
        String name = binding.edtTxtName.getText().toString().trim();
        String address = binding.edtTxtAddress.getText().toString().trim();

        // Kiểm tra trường nhập
        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || address.isEmpty()) {
            binding.txtWarning.setVisibility(View.VISIBLE);
            binding.txtWarning.setText("Email, mật khẩu, tên và địa chỉ không được trống");
        } else if (!email.endsWith("@gmail.com")) {
            binding.txtWarning.setVisibility(View.VISIBLE);
            binding.txtWarning.setText("Email phải là địa chỉ Gmail kết thúc bằng @gmail.com");
        } else if (password.length() < 6 || !password.matches(".*[!@#$%^&*()].*")) {
            binding.txtWarning.setVisibility(View.VISIBLE);
            binding.txtWarning.setText("Mật khẩu phải có ít nhất 6 ký tự và chứa ít nhất một ký tự đặc biệt");
        } else {
            binding.txtWarning.setVisibility(View.GONE);
            doesUserExistTask = new DoesUserExist();
            doesUserExistTask.execute(email);
        }
    }

    private class DoesUserExist extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            try (SQLiteDatabase db = databaseHelper.getReadableDatabase();
                 Cursor cursor = db.query("users", new String[]{"email"}, "email=?",
                         new String[]{strings[0]}, null, null, null)) {
                return cursor != null && cursor.moveToFirst();
            } catch (SQLException e) {
                e.printStackTrace();
                return true;
            }
        }

        @Override
        protected void onPostExecute(Boolean userExists) {
            if (userExists) {
                binding.txtWarning.setVisibility(View.VISIBLE);
                binding.txtWarning.setText("There is user with this email, please try another email");
            } else {
                registerUserTask = new RegisterUser();
                registerUserTask.execute();
            }
        }
    }

    private class RegisterUser extends AsyncTask<Void, Void, User> {
        private String email;
        private String password;
        private String address;
        private String first_name;
        private String last_name;

        @Override
        protected void onPreExecute() {
            email = binding.edtTxtEmail.getText().toString().trim();
            password = binding.edtTxtPassword.getText().toString().trim();
            address = binding.edtTxtAddress.getText().toString().trim();
            String name = binding.edtTxtName.getText().toString().trim();

            String[] names = name.split(" ");
            first_name = names[0];
            last_name = names.length > 1 ? name.substring(name.indexOf(" ") + 1) : "";
        }

        @SuppressLint("Range")
        @Override
        protected User doInBackground(Void... voids) {
            try (SQLiteDatabase db = databaseHelper.getWritableDatabase()) {
                ContentValues values = new ContentValues();
                values.put("email", email);
                values.put("password", password);
                values.put("address", address);
                values.put("first_name", first_name);
                values.put("last_name", last_name);
                values.put("remained_amount", 0.0);
                values.put("image_url", image_url);

                long userId = db.insert("users", null, values);
                if (userId == -1) {
                    return null;
                }

                try (Cursor cursor = db.query("users", null, "_id=?",
                        new String[]{String.valueOf(userId)}, null, null, null)) {
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
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(User user) {
            if (user != null) {
                Toast.makeText(RegisterActivity.this, "User " + user.getEmail() + " registered successfully", Toast.LENGTH_SHORT).show();
                Utils utils = new Utils(RegisterActivity.this);
                utils.addUserToSharedPreferences(user);
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(RegisterActivity.this, "Wasn't able to register, please try again later", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (doesUserExistTask != null && !doesUserExistTask.isCancelled()) {
            doesUserExistTask.cancel(true);
        }
        if (registerUserTask != null && !registerUserTask.isCancelled()) {
            registerUserTask.cancel(true);
        }
    }
}
