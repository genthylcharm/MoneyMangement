package com.dvl.moneymanagement.Activity;

import androidx.annotation.NonNull;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.dvl.moneymanagement.Activity.Base.BaseActivity;
import com.dvl.moneymanagement.DataBase.DatabaseHelper;
import com.dvl.moneymanagement.Model.Loan;
import com.dvl.moneymanagement.Model.Transaction;
import com.dvl.moneymanagement.Model.User;
import com.dvl.moneymanagement.R;
import com.dvl.moneymanagement.databinding.ActivityStatsBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.animation.Easing;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class StatsActivity extends BaseActivity {

    private static final String TAG = "StatsActivity";

    private ActivityStatsBinding binding; // Use the generated binding class

    private DatabaseHelper databaseHelper;
    private Utils utils;

    private GetLoans getLoans;
    private GetTransactions getTransactions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initBottomNavView();

        databaseHelper = new DatabaseHelper(this);
        utils = new Utils(this);

        User user = utils.isUserLoggedIn();
        if (null != user) {
            getTransactions = new GetTransactions();
            getTransactions.execute(user.get_id());
            getLoans = new GetLoans();
            getLoans.execute(user.get_id());
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != getTransactions) {
            if (!getTransactions.isCancelled()) {
                getTransactions.cancel(true);
            }
        }

        if (null != getLoans) {
            if (!getLoans.isCancelled()) {
                getLoans.cancel(true);
            }
        }
    }

    private class GetTransactions extends AsyncTask<Integer, Void, ArrayList<Transaction>> {
        @SuppressLint("Range")
        @Override
        protected ArrayList<Transaction> doInBackground(Integer... integers) {
            try {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor = db.query("transactions", null, null, null, null, null, null);
                if (null != cursor) {
                    if (cursor.moveToFirst()) {
                        ArrayList<Transaction> transactions = new ArrayList<>();
                        for (int i = 0; i < cursor.getCount(); i++) {
                            Transaction transaction = new Transaction();
                            transaction.set_id(cursor.getInt(cursor.getColumnIndex("_id")));
                            transaction.setUser_id(cursor.getInt(cursor.getColumnIndex("user_id")));
                            transaction.setType(cursor.getString(cursor.getColumnIndex("type")));
                            transaction.setDescription(cursor.getString(cursor.getColumnIndex("description")));
                            transaction.setRecipient(cursor.getString(cursor.getColumnIndex("recipient")));
                            transaction.setDate(cursor.getString(cursor.getColumnIndex("date")));
                            transaction.setAmount(cursor.getDouble(cursor.getColumnIndex("amount")));

                            transactions.add(transaction);
                            cursor.moveToNext();
                        }

                        cursor.close();
                        db.close();
                        return transactions;
                    } else {
                        cursor.close();
                        db.close();
                        return null;
                    }
                } else {
                    db.close();
                    return null;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Transaction> transactions) {
            super.onPostExecute(transactions);

            if (null != transactions) {

                for (Transaction t : transactions) {
                    Log.d(TAG, "onPostExecute: transactions: " + t.toString());
                }

                Calendar calendar = Calendar.getInstance();
                int currentMonth = calendar.get(Calendar.MONTH);
                int currentYear = calendar.get(Calendar.YEAR);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                ArrayList<BarEntry> entries = new ArrayList<>();
                for (Transaction t : transactions) {
                    try {
                        Date date = sdf.parse(t.getDate());
                        calendar.setTime(date);
                        int month = calendar.get(Calendar.MONTH);
                        int year = calendar.get(Calendar.YEAR);
                        int day = calendar.get(Calendar.DAY_OF_MONTH) + 1;

                        if (month == currentMonth && year == currentYear) {
                            boolean doesDayExist = false;

                            for (BarEntry e : entries) {
                                if (e.getX() == day) {
                                    doesDayExist = true;
                                } else {
                                    doesDayExist = false;
                                }
                            }

                            if (doesDayExist) {
                                for (BarEntry e : entries) {
                                    if (e.getX() == day) {
                                        e.setY(e.getY() + (float) t.getAmount());
                                    }
                                }
                            } else {
                                entries.add(new BarEntry(day, (float) t.getAmount()));
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                for (BarEntry e : entries) {
                    Log.d(TAG, "onPostExecute: x: " + e.getX() + " y: " + e.getY());
                }

                BarDataSet dataSet = new BarDataSet(entries, "Account Activity");
                dataSet.setColor(Color.GREEN);
                BarData data = new BarData(dataSet);

                binding.barChartActivities.getAxisRight().setEnabled(false);
                XAxis xAxis = binding.barChartActivities.getXAxis();
                xAxis.setAxisMaximum(31);
                xAxis.setEnabled(false);
                YAxis yAxis = binding.barChartActivities.getAxisLeft();
                yAxis.setAxisMinimum(10);
                yAxis.setDrawGridLines(false);
                binding.barChartActivities.setData(data);
                Description description = new Description();
                description.setText("All of the account transactions");
                description.setTextSize(12f);
                binding.barChartActivities.setDescription(description);
                binding.barChartActivities.invalidate();
            }
        }
    }

    private class GetLoans extends AsyncTask<Integer, Void, ArrayList<Loan>> {
        @SuppressLint("Range")
        @Override
        protected ArrayList<Loan> doInBackground(Integer... integers) {
            try {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor = db.query("loans", null, null, null, null, null, null);
                if (null != cursor) {
                    if (cursor.moveToFirst()) {
                        ArrayList<Loan> loans = new ArrayList<>();
                        for (int i = 0; i < cursor.getCount(); i++) {
                            Loan loan = new Loan();
                            loan.set_id(cursor.getInt(cursor.getColumnIndex("_id")));
                            loan.setUser_id(cursor.getInt(cursor.getColumnIndex("user_id")));
                            loan.setTransaction_id(cursor.getInt(cursor.getColumnIndex("transaction_id")));
                            loan.setName(cursor.getString(cursor.getColumnIndex("name")));
                            loan.setInit_date(cursor.getString(cursor.getColumnIndex("init_date")));
                            loan.setFinish_date(cursor.getString(cursor.getColumnIndex("finish_date")));
                            loan.setInit_amount(cursor.getDouble(cursor.getColumnIndex("init_amount")));
                            loan.setMonthly_roi(cursor.getDouble(cursor.getColumnIndex("monthly_roi")));
                            loan.setMonthly_payment(cursor.getDouble(cursor.getColumnIndex("monthly_payment")));
                            loan.setRemained_amount(cursor.getDouble(cursor.getColumnIndex("remained_amount")));
                            loans.add(loan);
                            cursor.moveToNext();
                        }

                        cursor.close();
                        db.close();
                        return loans;
                    } else {
                        cursor.close();
                        db.close();
                        return null;
                    }
                } else {
                    db.close();
                    return null;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Loan> loans) {
            super.onPostExecute(loans);

            if (null != loans) {
                ArrayList<PieEntry> entries = new ArrayList<>();
                double totalLoansAmount = 0.0;
                double totalRemainedAmount = 0.0;

                for (Loan l : loans) {
                    totalLoansAmount += l.getInit_amount();
                    totalRemainedAmount += l.getRemained_amount();
                }

                entries.add(new PieEntry((float) totalLoansAmount, "Total Loans"));
                entries.add(new PieEntry((float) 5000, "Remained Loans"));
                PieDataSet dataSet = new PieDataSet(entries, "");
                dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
                dataSet.setSliceSpace(5f);
                PieData data = new PieData(dataSet);
                binding.pieChartLoans.setDrawHoleEnabled(false);
                binding.pieChartLoans.animateY(2000, Easing.EaseInOutBack);
                binding.pieChartLoans.setData(data);
                binding.pieChartLoans.invalidate();
            }
        }
    }

    private void initBottomNavView() {
        Log.d(TAG, "initBottomNavView: started");
        binding.bottomNavView.setSelectedItemId(R.id.menu_item_stats);
        binding.bottomNavView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_item_stats) {
                    // Handle stats action if needed
                } else if (menuItem.getItemId() == R.id.menu_item_transaction) {
                    Intent transactionIntent = new Intent(StatsActivity.this, TransactionActivity.class);
                    transactionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(transactionIntent);
                } else if (menuItem.getItemId() == R.id.menu_item_home) {
                    Intent homeIntent = new Intent(StatsActivity.this, MainActivity.class);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(homeIntent);
                } else if (menuItem.getItemId() == R.id.menu_item_loan) {
                    Intent loanIntent = new Intent(StatsActivity.this, LoanActivity.class);
                    loanIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loanIntent);
                } else if (menuItem.getItemId() == R.id.menu_item_investment) {
                    Intent intent = new Intent(StatsActivity.this, InvestmentActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                return false;
            }
        });
    }
}
