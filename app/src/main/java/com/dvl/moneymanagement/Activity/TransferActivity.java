package com.dvl.moneymanagement.Activity;

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
import android.widget.RadioGroup;
import android.widget.TextView;

import com.dvl.moneymanagement.Activity.Base.BaseActivity;
import com.dvl.moneymanagement.DataBase.DatabaseHelper;
import com.dvl.moneymanagement.Model.User;
import com.dvl.moneymanagement.R;
import com.dvl.moneymanagement.databinding.ActivityTransferBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TransferActivity extends BaseActivity {

    private static final String TAG = "TransferActivity";
    ActivityTransferBinding binding;
    private Calendar calendar = Calendar.getInstance();

    private DatePickerDialog.OnDateSetListener initDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
            calendar.set(Calendar.YEAR, i);
            calendar.set(Calendar.MONTH, i1);
            calendar.set(Calendar.DAY_OF_MONTH, i2);
            binding.edtTxtDate.setText(new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));
        }
    };

    private AddTransaction addTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransferBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setOnClickListeners();
    }

    private void setOnClickListeners() {
        Log.d(TAG, "setOnClickListeners: started");
        binding.btnPickDate.setOnClickListener(view -> new DatePickerDialog(TransferActivity.this, initDateSetListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show());

        binding.btnAdd.setOnClickListener(view -> {
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
        Log.d(TAG, "initAdding: started");
        Utils utils = new Utils(this);
        User user = utils.isUserLoggedIn();
        if (null != user) {

            addTransaction = new AddTransaction();
            addTransaction.execute(user.get_id());
        }
    }

    private class AddTransaction extends AsyncTask<Integer, Void, Void> {

        private double amount;
        private String recipient, date, description, type;

        private DatabaseHelper databaseHelper;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            this.recipient = binding.edtTxtRecipient.getText().toString();
            this.date = binding.edtTxtDate.getText().toString();
            this.description = binding.edtTxtDescription.getText().toString();
            this.amount = Double.valueOf(binding.edtTxtAmount.getText().toString());

            int rbId = binding.rgType.getCheckedRadioButtonId();
            if (rbId == R.id.btnReceive) {
                type = "receive";
            } else if (rbId == R.id.btnSend) {
                type = "send";
                amount = -amount;
            } else {
                // Handle default case if needed
            }

            databaseHelper = new DatabaseHelper(TransferActivity.this);

        }

        @Override
        protected Void doInBackground(Integer... integers) {

            try {
                SQLiteDatabase db = databaseHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("amount", this.amount);
                values.put("recipient", recipient);
                values.put("date", date);
                values.put("type", type);
                values.put("description", description);
                values.put("user_id", integers[0]);

                Log.d(TAG, "doInBackground: amount: " + amount);

                long id = db.insert("transactions", null, values);
                Log.d(TAG, "doInBackground: new Transaction id: " + id);
                if (id != -1) {
                    Cursor cursor = db.query("users", new String[]{"remained_amount"}, "_id=?",
                            new String[]{String.valueOf(integers[0])}, null, null, null);
                    if (null != cursor) {
                        if (cursor.moveToFirst()) {
                            @SuppressLint("Range") double currentRemainedAmount = cursor.getDouble(cursor.getColumnIndex("remained_amount"));
                            cursor.close();
                            ContentValues newValues = new ContentValues();
                            newValues.put("remained_amount", currentRemainedAmount + this.amount);
                            int affectedRows = db.update("users", newValues, "_id=?", new String[]{String.valueOf(integers[0])});
                            Log.d(TAG, "doInBackground: updatedRows: " + affectedRows);
                            db.close();
                        } else {
                            cursor.close();
                            db.close();
                        }
                    } else {
                        db.close();
                    }
                } else {
                    db.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Intent intent = new Intent(TransferActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != addTransaction) {
            if (!addTransaction.isCancelled()) {
                addTransaction.cancel(true);
            }
        }
    }

    private boolean validateData() {
        Log.d(TAG, "validateData: started");
        if (binding.edtTxtAmount.getText().toString().equals("")) {
            return false;
        }

        if (binding.edtTxtDate.getText().toString().equals("")) {
            return false;
        }

        if (binding.edtTxtRecipient.getText().toString().equals("")) {
            return false;
        }
        return true;
    }
}