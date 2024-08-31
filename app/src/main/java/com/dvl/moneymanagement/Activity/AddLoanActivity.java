package com.dvl.moneymanagement.Activity;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Constraints;
import androidx.work.WorkManager;

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
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.dvl.moneymanagement.Activity.Base.BaseActivity;
import com.dvl.moneymanagement.DataBase.DatabaseHelper;
import com.dvl.moneymanagement.Model.User;
import com.dvl.moneymanagement.R;
import com.dvl.moneymanagement.databinding.ActivityAddLoanBinding;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AddLoanActivity extends BaseActivity {

    private static final String TAG = "AddLoanActivity";

    private ActivityAddLoanBinding binding;

    private Calendar initCalendar = Calendar.getInstance();
    private Calendar finishCalendar = Calendar.getInstance();

    private DatePickerDialog.OnDateSetListener initDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
            initCalendar.set(Calendar.YEAR, year);
            initCalendar.set(Calendar.MONTH, month);
            initCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            binding.edtTxtInitDate.setText(new SimpleDateFormat("yyyy-MM-dd").format(initCalendar.getTime()));
        }
    };

    private DatePickerDialog.OnDateSetListener finishDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
            finishCalendar.set(Calendar.YEAR, year);
            finishCalendar.set(Calendar.MONTH, month);
            finishCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            binding.edtTxtFinishDate.setText(new SimpleDateFormat("yyyy-MM-dd").format(finishCalendar.getTime()));
        }
    };

    private Utils utils;
    private DatabaseHelper databaseHelper;
    private AddTransaction addTransaction;
    private AddLoan addLoan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddLoanBinding.inflate(getLayoutInflater()); // Inflate binding
        setContentView(binding.getRoot()); // Set content view to the root of the binding

        databaseHelper = new DatabaseHelper(this);
        utils = new Utils(this);

        setOnClickListeners();
    }

    private void setOnClickListeners() {
        Log.d(TAG, "setOnClickListeners: started");

        binding.btnPickInitDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(AddLoanActivity.this, initDateSetListener,
                        initCalendar.get(Calendar.YEAR), initCalendar.get(Calendar.MONTH), initCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        binding.btnPickFinishDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(AddLoanActivity.this, finishDateSetListener,
                        finishCalendar.get(Calendar.YEAR), finishCalendar.get(Calendar.MONTH), finishCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        binding.btnAddLoan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateData()) {
                    binding.txtWarning.setVisibility(View.GONE);
                    initAdding();
                } else {
                    binding.txtWarning.setVisibility(View.VISIBLE);
                    binding.txtWarning.setText("Please fill all the blanks");
                }
            }
        });
    }

    private void initAdding() {
        Log.d(TAG, "initAdding: started");

        User user = utils.isUserLoggedIn();
        if (null != user) {
            addTransaction = new AddTransaction();
            addTransaction.execute(user.get_id());
        }
    }

    private class AddTransaction extends AsyncTask<Integer, Void, Integer> {

        private double amount;
        private String date, name;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.amount = Double.valueOf(binding.edtTxtInitAmount.getText().toString());
            this.name = binding.edtTxtName.getText().toString();
            this.date = binding.edtTxtInitDate.getText().toString();
        }

        @Override
        protected Integer doInBackground(Integer... integers) {
            try {
                SQLiteDatabase db = databaseHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("amount", amount);
                values.put("recipient", name);
                values.put("date", date);
                values.put("user_id", integers[0]);
                values.put("description", "Received amount for " + name + " Loan");
                values.put("type", "loan");
                long transactionId = db.insert("transactions", null, values);
                db.close();
                return (int) transactionId;
            } catch (SQLException e) {
                Log.e(TAG, "Database error while adding transaction", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);

            if (null != integer) {
                String formattedAmount = NumberFormat.getInstance(Locale.US).format(amount);
                Log.d(TAG, "Transaction added with amount: " + formattedAmount);

                addLoan = new AddLoan();
                addLoan.execute(integer);
            }
        }
    }

    private class AddLoan extends AsyncTask<Integer, Void, Integer> {

        private int userId;
        private String name, initDate, finishDate;
        private double initAmount, monthlyROI, monthlyPayment;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.name = binding.edtTxtName.getText().toString();
            this.initDate = binding.edtTxtInitDate.getText().toString();
            this.finishDate = binding.edtTxtFinishDate.getText().toString();
            this.initAmount = Double.valueOf(binding.edtTxtInitAmount.getText().toString());
            this.monthlyROI = Double.valueOf(binding.edtTxtMonthlyROI.getText().toString());
            this.monthlyPayment = Double.valueOf(binding.edtTxtMonthlyPayment.getText().toString());
            User user = utils.isUserLoggedIn();
            if (null != user) {
                this.userId = user.get_id();
            } else {
                this.userId = -1;
            }
        }

        @Override
        protected Integer doInBackground(Integer... integers) {
            if (userId != -1) {
                try {
                    SQLiteDatabase db = databaseHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("name", name);
                    values.put("init_date", initDate);
                    values.put("finish_date", finishDate);
                    values.put("init_amount", initAmount);
                    values.put("remained_amount", initAmount);
                    values.put("monthly_roi", monthlyROI);
                    values.put("monthly_payment", monthlyPayment);
                    values.put("user_id", userId);
                    values.put("transaction_id", integers[0]);

                    long loanId = db.insert("loans", null, values);

                    if (loanId != -1) {
                        Cursor cursor = db.query("users", new String[]{"remained_amount"}, "_id=?",
                                new String[]{String.valueOf(userId)}, null, null, null);
                        if (null != cursor) {
                            if (cursor.moveToFirst()) {
                                @SuppressLint("Range") double currentRemainedAmount = cursor.getDouble(cursor.getColumnIndex("remained_amount"));
                                ContentValues newValues = new ContentValues();
                                newValues.put("remained_amount", currentRemainedAmount + initAmount);
                                db.update("users", newValues, "_id=?", new String[]{String.valueOf(userId)});
                                cursor.close();
                                db.close();
                                return (int) loanId;
                            } else {
                                cursor.close();
                                db.close();
                                return null;
                            }
                        } else {
                            db.close();
                            return null;
                        }
                    } else {
                        db.close();
                        return null;
                    }
                } catch (SQLException e) {
                    Log.e(TAG, "Database error while adding loan", e);
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);

            if (null != integer) {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Date initDate = sdf.parse(this.initDate);
                    calendar.setTime(initDate);
                    int initMonth = calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH);

                    Date finishDate = sdf.parse(this.finishDate);
                    calendar.setTime(finishDate);
                    int finishMonth = calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH);

                    int months = finishMonth - initMonth;

                    int days = 0;
                    for (int i = 0; i < months; i++) {
                        days += 30;

                        Data data = new Data.Builder()
                                .putDouble("monthlyPayment", monthlyPayment)
                                .putDouble("initAmount", initAmount)
                                .putDouble("monthlyROI", monthlyROI)
                                .putString("name", name)
                                .build();

                        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(LoanWorker.class)
                                .setInputData(data)
                                .setInitialDelay(days, TimeUnit.DAYS)
                                .build();

                        WorkManager.getInstance(AddLoanActivity.this).enqueue(workRequest);
                    }

                    Intent intent = new Intent(AddLoanActivity.this, LoanActivity.class);
                    intent.putExtra("success", "Added loan successfully");
                    startActivity(intent);
                    finish();

                } catch (ParseException e) {
                    Log.e(TAG, "Date parsing error", e);
                }
            } else {
                binding.txtWarning.setVisibility(View.VISIBLE);
                binding.txtWarning.setText("Failed to add loan");
            }
        }
    }

    private boolean validateData() {
        return !binding.edtTxtName.getText().toString().isEmpty() &&
                !binding.edtTxtInitAmount.getText().toString().isEmpty() &&
                !binding.edtTxtMonthlyROI.getText().toString().isEmpty() &&
                !binding.edtTxtInitDate.getText().toString().isEmpty() &&
                !binding.edtTxtFinishDate.getText().toString().isEmpty() &&
                !binding.edtTxtMonthlyPayment.getText().toString().isEmpty();
    }
}

