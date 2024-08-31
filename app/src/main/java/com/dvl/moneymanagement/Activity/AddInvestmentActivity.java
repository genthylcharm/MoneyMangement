package com.dvl.moneymanagement.Activity;

import androidx.work.Constraints;
import androidx.work.WorkManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.dvl.moneymanagement.Activity.Base.BaseActivity;
import com.dvl.moneymanagement.DataBase.DatabaseHelper;
import com.dvl.moneymanagement.Model.User;
import com.dvl.moneymanagement.databinding.ActivityAddInvestmentBinding;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AddInvestmentActivity extends BaseActivity {

    private static final String TAG = "AddInvestmentActivity";

    private ActivityAddInvestmentBinding binding;

    private Calendar initCalendar = Calendar.getInstance();
    private Calendar finishCalendar = Calendar.getInstance();

    private DatabaseHelper databaseHelper;
    private Utils utils;

    private DatePickerDialog.OnDateSetListener initDateSetListener = (datePicker, year, month, day) -> {
        initCalendar.set(Calendar.YEAR, year);
        initCalendar.set(Calendar.MONTH, month);
        initCalendar.set(Calendar.DAY_OF_MONTH, day);
        binding.edtTxtInitDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(initCalendar.getTime()));
    };

    private DatePickerDialog.OnDateSetListener finishDateSetListener = (datePicker, year, month, day) -> {
        finishCalendar.set(Calendar.YEAR, year);
        finishCalendar.set(Calendar.MONTH, month);
        finishCalendar.set(Calendar.DAY_OF_MONTH, day);
        binding.edtTxtFinishDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(finishCalendar.getTime()));
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddInvestmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        utils = new Utils(this);
        databaseHelper = new DatabaseHelper(this);
        setOnClickListeners();
    }

    private void setOnClickListeners() {
        binding.btnPickInitDate.setOnClickListener(view -> new DatePickerDialog(this, initDateSetListener,
                initCalendar.get(Calendar.YEAR), initCalendar.get(Calendar.MONTH), initCalendar.get(Calendar.DAY_OF_MONTH)).show());

        binding.btnPickFinishDate.setOnClickListener(view -> new DatePickerDialog(this, finishDateSetListener,
                finishCalendar.get(Calendar.YEAR), finishCalendar.get(Calendar.MONTH), finishCalendar.get(Calendar.DAY_OF_MONTH)).show());

        binding.btnAddInvestment.setOnClickListener(view -> {
            if (validateData()) {
                binding.txtWarning.setVisibility(View.GONE);
                initAdding();
            } else {
                binding.txtWarning.setVisibility(View.VISIBLE);
                binding.txtWarning.setText("Please fill all the blanks");
            }
        });
    }

    private void initAdding() {
        new AddTransaction().execute(utils.isUserLoggedIn().get_id());
    }

    private class AddTransaction extends AsyncTask<Integer, Void, Integer> {
        private String date, name;
        private double amount;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            date = binding.edtTxtInitDate.getText().toString();
            name = binding.edtTxtName.getText().toString();
            amount = -Double.parseDouble(binding.edtTxtInitAmount.getText().toString());
        }

        @Override
        protected Integer doInBackground(Integer... integers) {
            try (SQLiteDatabase db = databaseHelper.getWritableDatabase()) {
                ContentValues values = new ContentValues();
                values.put("amount", amount);
                values.put("recipient", name);
                values.put("date", date);
                values.put("description", "Initial amount for " + name + " investment");
                values.put("user_id", integers[0]);
                values.put("type", "investment");
                long id = db.insert("transactions", null, values);
                return (int) id;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer transactionId) {
            if (transactionId != null) {
                new AddInvestment().execute(transactionId);
            }
        }
    }

    private class AddInvestment extends AsyncTask<Integer, Void, Void> {
        private int userId;
        private String initDate, finishDate, name;
        private double monthlyROI, amount;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            amount = Double.parseDouble(binding.edtTxtInitAmount.getText().toString());
            monthlyROI = Double.parseDouble(binding.edtTxtMonthlyROI.getText().toString());
            name = binding.edtTxtName.getText().toString();
            initDate = binding.edtTxtInitDate.getText().toString();
            finishDate = binding.edtTxtFinishDate.getText().toString();
            User user = utils.isUserLoggedIn();
            userId = (user != null) ? user.get_id() : -1;
        }

        @Override
        protected Void doInBackground(Integer... integers) {
            if (userId != -1) {
                try (SQLiteDatabase db = databaseHelper.getWritableDatabase()) {
                    ContentValues values = new ContentValues();
                    values.put("name", name);
                    values.put("init_date", initDate);
                    values.put("finish_date", finishDate);
                    values.put("amount", amount);
                    values.put("monthly_roi", monthlyROI);
                    values.put("user_id", userId);
                    values.put("transaction_id", integers[0]);
                    long id = db.insert("investments", null, values);
                    if (id != -1) {
                        updateUserRemainingAmount(db);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        private void updateUserRemainingAmount(SQLiteDatabase db) {
            try (Cursor cursor = db.query("users", new String[]{"remained_amount"}, "_id=?",
                    new String[]{String.valueOf(userId)}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    @SuppressLint("Range") double currentRemainedAmount = cursor.getDouble(cursor.getColumnIndex("remained_amount"));
                    ContentValues newValues = new ContentValues();
                    newValues.put("remained_amount", currentRemainedAmount - amount);
                    db.update("users", newValues, "_id=?", new String[]{String.valueOf(userId)});
                    Log.d(TAG, "Updated remaining amount: " + formatAmount(currentRemainedAmount - amount));
                }
            }
        }

        private String formatAmount(double amount) {
            NumberFormat numberFormat = NumberFormat.getInstance(Locale.getDefault());
            numberFormat.setMinimumFractionDigits(0);
            numberFormat.setMaximumFractionDigits(2);
            return numberFormat.format(amount);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            scheduleInvestmentProfits();
            startActivity(new Intent(AddInvestmentActivity.this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }

        private void scheduleInvestmentProfits() {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                Date initDateParsed = sdf.parse(initDate);
                calendar.setTime(initDateParsed);
                int initMonths = calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH);
                Date finishDateParsed = sdf.parse(finishDate);
                calendar.setTime(finishDateParsed);
                int finishMonths = calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH);

                int difference = finishMonths - initMonths;
                for (int i = 0; i < difference; i++) {
                    scheduleProfitTask(i);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        private void scheduleProfitTask(int month) {
            Constraints constraints = new Constraints.Builder().build();
            Data data = new Data.Builder()
                    .putInt("transaction_id", month)
                    .build();
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(InvestmentWorker.class)
                    .setInputData(data)
                    .setConstraints(constraints)
                    .setInitialDelay(month + 1, TimeUnit.DAYS) // Delay 1 day for each month
                    .build();
            WorkManager.getInstance(AddInvestmentActivity.this).enqueue(workRequest);
        }
    }

    private boolean validateData() {
        return !binding.edtTxtName.getText().toString().isEmpty() &&
                !binding.edtTxtInitAmount.getText().toString().isEmpty() &&
                !binding.edtTxtMonthlyROI.getText().toString().isEmpty() &&
                !binding.edtTxtInitDate.getText().toString().isEmpty() &&
                !binding.edtTxtFinishDate.getText().toString().isEmpty();
    }
}
