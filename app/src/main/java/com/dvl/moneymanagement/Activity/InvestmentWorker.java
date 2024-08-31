package com.dvl.moneymanagement.Activity;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.dvl.moneymanagement.DataBase.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class InvestmentWorker extends Worker {
    private static final String TAG = "InvestmentWorker";

    private final DatabaseHelper databaseHelper;

    public InvestmentWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        databaseHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork: called");

        double amount = getInputData().getDouble("amount", 0.0);
        String recipient = getInputData().getString("recipient");
        String description = getInputData().getString("description");
        int user_id = getInputData().getInt("user_id", -1);
        String type = "profit";

        // Lấy ngày hiện tại
        String date = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());

        ContentValues values = new ContentValues();
        values.put("amount", amount);
        values.put("recipient", recipient);
        values.put("description", description);
        values.put("user_id", user_id);
        values.put("type", type);
        values.put("date", date);

        SQLiteDatabase db = null;
        try {
            db = databaseHelper.getWritableDatabase();
            long id = db.insert("transactions", null, values);

            if (id != -1) {
                // Cập nhật số tiền còn lại của người dùng
                updateUserRemainedAmount(db, user_id, amount);
            } else {
                Log.e(TAG, "Failed to insert transaction");
            }
        } catch (SQLException e) {
            Log.e(TAG, "Database error", e);
            return Result.failure();
        } finally {
            if (db != null) {
                db.close();
            }
        }

        return Result.success();
    }

    private void updateUserRemainedAmount(SQLiteDatabase db, int userId, double amount) {
        Cursor cursor = null;
        try {
            cursor = db.query("users", new String[]{"remained_amount"}, "_id=?",
                    new String[]{String.valueOf(userId)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") double currentRemainedAmount = cursor.getDouble(cursor.getColumnIndex("remained_amount"));

                ContentValues newValues = new ContentValues();
                newValues.put("remained_amount", currentRemainedAmount - amount);
                int affectedRows = db.update("users", newValues, "_id=?", new String[]{String.valueOf(userId)});
                Log.d(TAG, "updateUserRemainedAmount: updatedRows: " + affectedRows);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Error updating user remained amount", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
