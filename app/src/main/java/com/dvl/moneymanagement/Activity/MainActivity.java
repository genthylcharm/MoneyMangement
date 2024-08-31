package com.dvl.moneymanagement.Activity;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkManager;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import com.dvl.moneymanagement.Activity.Base.BaseActivity;
import com.dvl.moneymanagement.Adapters.TransactionAdapter;
import com.dvl.moneymanagement.Activity.Authentication.LoginActivity;
import com.dvl.moneymanagement.DataBase.DatabaseHelper;
import com.dvl.moneymanagement.Activity.Dialog.AddTransactionDialog;
import com.dvl.moneymanagement.Model.Shopping;
import com.dvl.moneymanagement.Model.Transaction;
import com.dvl.moneymanagement.Model.User;
import com.dvl.moneymanagement.R;
import com.dvl.moneymanagement.databinding.ActivityMainBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;

    private Utils utils;
    private DatabaseHelper databaseHelper;

    private GetAccountAmount getAccountAmount;
    private GetTransactions getTransactions;
    private GetProfit getProfit;
    private GetSpending getSpending;
    private TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initBottomNavView();
        Toolbar toolbar = binding.toolbar; // Access toolbar directly from binding
        setSupportActionBar(toolbar);

        utils = new Utils(this);
        User user = utils.isUserLoggedIn();
        if (null != user) {
            Toast.makeText(this, "User: " + user.getFirst_name() + " logged in", Toast.LENGTH_SHORT).show();
        }else {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        databaseHelper = new DatabaseHelper(this);

        setupAmount();
        setOnClickListeners();
        initTransactionRecView();
        initLineChart();
        initBarChart();

        Log.d(TAG, "onCreate: work: " + WorkManager.getInstance(this).getWorkInfosByTag("profit"));
        Log.d(TAG, "onCreate: loan work: " + WorkManager.getInstance(this).getWorkInfosByTag("loan_payment"));
    }

    private void initBarChart() {
        Log.d(TAG, "initBarChart: started");

        getSpending = new GetSpending();
        User user = utils.isUserLoggedIn();
        if (null != user) {
            getSpending.execute(user.get_id());
        }
    }

    private void initLineChart() {
        Log.d(TAG, "initLineChart: started");

        getProfit = new GetProfit();
        User user = utils.isUserLoggedIn();
        if (null != user) {
            getProfit.execute(user.get_id());
        }
    }

    private void initTransactionRecView() {
        Log.d(TAG, "initTransactionRecView: started");
        adapter = new TransactionAdapter();
        binding.transactionRecView.setAdapter(adapter);
        binding.transactionRecView.setLayoutManager(new LinearLayoutManager(this));
        getTransactions();
    }

    private void getTransactions() {
        Log.d(TAG, "getTransactions: started");
        getTransactions = new GetTransactions();
        User user = utils.isUserLoggedIn();
        if (null != user) {
            getTransactions.execute(user.get_id());
        }
    }

    private void setOnClickListeners() {
        Log.d(TAG, "setOnClickListeners: started");
        binding.txtWelcome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Mei Bank")
                        .setMessage("Created and Developed By Meisam at MeiCode.org")
                        .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }).setPositiveButton("Visit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(MainActivity.this, WebsiteActivity.class);
                                startActivity(intent);
                            }
                        });
                builder.show();
            }
        });

        binding.fbAddTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddTransactionDialog addTransactionDialog = new AddTransactionDialog();
                addTransactionDialog.show(getSupportFragmentManager(), "add transaction dialog");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupAmount();
        getTransactions();
        initLineChart();
        initBarChart();
    }

    @Override
    protected void onStart() {
        super.onStart();

        setupAmount();
        getTransactions();
        initLineChart();
        initBarChart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != getTransactions) {
            if (!getTransactions.isCancelled()) {
                getTransactions.cancel(true);
            }
        }

        if (null != getAccountAmount) {
            if (!getAccountAmount.isCancelled()) {
                getAccountAmount.cancel(true);
            }
        }

        if (null != getProfit) {
            if (!getProfit.isCancelled()) {
                getProfit.cancel(true);
            }
        }

        if (null != getSpending) {
            if (!getSpending.isCancelled()) {
                getSpending.cancel(true);
            }
        }
    }

    private void setupAmount() {
        Log.d(TAG, "setupAmount: started");
        User user = utils.isUserLoggedIn();
        if (null != user) {
            getAccountAmount = new GetAccountAmount();
            getAccountAmount.execute(user.get_id());
        }
    }

    private class GetAccountAmount extends AsyncTask<Integer, Void, Double> {
        @Override
        protected Double doInBackground(Integer... integers) {
            try{
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor = db.query("users", new String[] {"remained_amount"}, "_id=?",
                        new String[] {String.valueOf(integers[0])}, null, null, null);
                if (null != cursor) {
                    if (cursor.moveToFirst()) {
                        @SuppressLint("Range") double amount = cursor.getDouble(cursor.getColumnIndex("remained_amount"));
                        cursor.close();
                        db.close();
                        return amount;
                    }else {
                        cursor.close();
                        db.close();
                        return null;
                    }
                }else {
                    db.close();
                    return null;
                }
            }catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Double aDouble) {
            super.onPostExecute(aDouble);

            if (null != aDouble) {
                binding.txtAmount.setText(aDouble + " $");
            }else {
                binding.txtAmount.setText("0.0 $");
            }
        }
    }
// lấy dữ liệu từ SQLITE
    private class GetTransactions extends AsyncTask<Integer, Void, ArrayList<Transaction>> {
        @SuppressLint("Range")
        @Override
        protected ArrayList<Transaction> doInBackground(Integer... integers) {
            try {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor = db.query("transactions", null, "user_id=?",
                        new String[] {String.valueOf(integers[0])}, null, null, "date DESC");
                if (null != cursor) {
                    if (cursor.moveToFirst()) {
                        ArrayList<Transaction> transactions = new ArrayList<>();
                        for (int i=0; i<cursor.getCount(); i++) {
                            Transaction transaction = new Transaction();
                            transaction.set_id(cursor.getInt(cursor.getColumnIndex("_id")));
                            transaction.setAmount(cursor.getDouble(cursor.getColumnIndex("amount")));
                            transaction.setDate(cursor.getString(cursor.getColumnIndex("date")));
                            transaction.setDescription(cursor.getString(cursor.getColumnIndex("description")));
                            transaction.setRecipient(cursor.getString(cursor.getColumnIndex("recipient")));
                            transaction.setType(cursor.getString(cursor.getColumnIndex("type")));
                            transaction.setUser_id(cursor.getInt(cursor.getColumnIndex("user_id")));
                            transactions.add(transaction);
                            cursor.moveToNext();
                        }

                        cursor.close();
                        db.close();
                        return transactions;
                    }else {
                        cursor.close();
                        db.close();
                        return null;
                    }
                }else {
                    db.close();
                    return null;
                }
            }catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Transaction> transactions) {
            super.onPostExecute(transactions);
            if (null != transactions) {
                adapter.setTransactions(transactions);
            }else {
                adapter.setTransactions(new ArrayList<Transaction>());
            }
        }
    }

    private class GetProfit extends AsyncTask<Integer, Void, ArrayList<Transaction>> {
        @SuppressLint("Range")
        @Override
        protected ArrayList<Transaction> doInBackground(Integer... integers) {
            try {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor = db.query("transactions", null, "user_id=? AND type=?",
                        new String[] {String.valueOf(integers[0]), "profit"}, null, null, null);

                if (null != cursor) {
                    if (cursor.moveToFirst()) {
                        ArrayList<Transaction> transactions = new ArrayList<>();
                        for (int i=0; i<cursor.getCount(); i++) {
                            Transaction transaction = new Transaction();
                            transaction.set_id(cursor.getInt(cursor.getColumnIndex("_id")));
                            transaction.setAmount(cursor.getDouble(cursor.getColumnIndex("amount")));
                            transaction.setDate(cursor.getString(cursor.getColumnIndex("date")));
                            transaction.setDescription(cursor.getString(cursor.getColumnIndex("description")));
                            transaction.setRecipient(cursor.getString(cursor.getColumnIndex("recipient")));
                            transaction.setType(cursor.getString(cursor.getColumnIndex("type")));
                            transaction.setUser_id(cursor.getInt(cursor.getColumnIndex("user_id")));
                            transactions.add(transaction);
                            cursor.moveToNext();
                        }

                        cursor.close();
                        db.close();
                        return transactions;
                    }else {
                        cursor.close();
                        db.close();
                        return null;
                    }
                }else {
                    db.close();
                    return null;
                }

            }catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Transaction> transactions) {
            super.onPostExecute(transactions);

            if (null != transactions) {
                ArrayList<Entry> entries = new ArrayList<>();

                for (Transaction t: transactions) {
                    try {
                        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(t.getDate());

                        Calendar calendar = Calendar.getInstance();
                        int year = calendar.get(Calendar.YEAR);
                        calendar.setTime(date);
                        int month= calendar.get(Calendar.MONTH)+1;
                        Log.d(TAG, "onPostExecute: month: " + month);

                        if (calendar.get(Calendar.YEAR) == year) {

                            boolean doesMonthExist =false;

                            for (Entry e: entries) {
                                doesMonthExist = e.getX() == month;
                            }

                            if (!doesMonthExist) {
                                entries.add(new Entry(month, (float) t.getAmount()));
                            }else {
                                for (Entry e: entries) {
                                    if (e.getX() == month) {
                                        e.setY(e.getY() + (float) t.getAmount());
                                    }
                                }
                            }
                        }


                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                for (Entry e: entries) {
                    Log.d(TAG, "onPostExecute: x: " + e.getX() + " y: " + e.getY());
                }

                LineDataSet dataSet = new LineDataSet(entries, "Profit chart");
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                dataSet.setDrawFilled(true);
                dataSet.setFillColor(Color.GREEN);
                LineData data = new LineData(dataSet);
                XAxis xAxis = binding.profitChart.getXAxis();
                xAxis.setSpaceMin(1);
                xAxis.setSpaceMax(1);
                xAxis.setAxisMaximum(12);
                xAxis.setEnabled(false);
                YAxis yAxis = binding.profitChart.getAxisRight();
                yAxis.setEnabled(false);
                YAxis leftAxis = binding.profitChart.getAxisLeft();
                leftAxis.setAxisMaximum(100);
                leftAxis.setAxisMinimum(10);
                leftAxis.setDrawGridLines(false);
                binding.profitChart.setDescription(null);
                binding.profitChart.animateY(2000);
                binding.profitChart.setData(data);
                binding.profitChart.invalidate();

            }else {
                Log.d(TAG, "onPostExecute: transactions array list was null");
            }
        }
    }

    private class GetSpending extends AsyncTask<Integer, Void, ArrayList<Shopping>> {
        @SuppressLint("Range")
        @Override
        protected ArrayList<Shopping> doInBackground(Integer... integers) {
            try {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor = db.query("shopping", new String[] {"date", "price"}, "user_id=?",
                        new String[] {String.valueOf(integers[0])}, null, null, null);

                if (null != cursor) {
                    if (cursor.moveToNext()) {
                        ArrayList<Shopping> shoppings = new ArrayList<>();
                        for (int i=0; i<cursor.getCount(); i++) {
                            Shopping shopping = new Shopping();
                            shopping.setDate(cursor.getString(cursor.getColumnIndex("date")));
                            shopping.setPrice(cursor.getDouble(cursor.getColumnIndex("price")));
                            shoppings.add(shopping);
                            cursor.moveToNext();
                        }

                        cursor.close();
                        db.close();
                        return shoppings;
                    }else {
                        cursor.close();
                        db.close();
                        return null;
                    }
                }else {
                    db.close();
                    return null;
                }

            }catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Shopping> shoppings) {
            super.onPostExecute(shoppings);

            Log.d(TAG, "onPostExecute: started");

            if (null != shoppings) {

                ArrayList<BarEntry> entries = new ArrayList<>();
                for (Shopping s: shoppings) {
                    try {
                        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(s.getDate());
                        Calendar calendar = Calendar.getInstance();
                        int month = calendar.get(Calendar.MONTH)+1;
                        calendar.setTime(date);
                        int day = calendar.get(Calendar.DAY_OF_MONTH)+1;

                        if (calendar.get(Calendar.MONTH)+1 == month) {
                            boolean doesDayExist = false;
                            for (BarEntry e: entries) {
                                doesDayExist = e.getX() == day;
                            }

                            if (!doesDayExist) {
                                entries.add(new BarEntry(day, (float) s.getPrice()));
                            }else {
                                for (BarEntry e: entries) {
                                    if (e.getX() == day) {
                                        e.setY(e.getY() + (float) + s.getPrice());
                                    }
                                }
                            }
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                for (BarEntry e: entries) {
                    Log.d(TAG, "onPostExecute: x: " + e.getX() + " y: " + e.getY());
                }

                BarDataSet dataSet = new BarDataSet(entries, "Shopping chart");
                dataSet.setColor(Color.RED);
                BarData data = new BarData(dataSet);

                binding.dailySpentChart.getAxisRight().setEnabled(false);
                XAxis xAxis = binding.dailySpentChart.getXAxis();
                xAxis.setAxisMaximum(31);
                xAxis.setEnabled(false);
                YAxis yAxis = binding.dailySpentChart.getAxisLeft();
                yAxis.setAxisMaximum(40);
                yAxis.setAxisMinimum(10);
                yAxis.setDrawGridLines(false);
                binding.dailySpentChart.setData(data);
                binding.dailySpentChart.setDescription(null);
                binding.dailySpentChart.invalidate();

            }else {
                Log.d(TAG, "onPostExecute: shoppings arraylist is null");
            }
        }
    }

    private void initBottomNavView() {
        Log.d(TAG, "initBottomNavView: started");
        binding.bottomNavView.setSelectedItemId(R.id.menu_item_home);
        binding.bottomNavView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Intent intent = null;
                if (menuItem.getItemId() == R.id.menu_item_stats) {
                    intent = new Intent(MainActivity.this, StatsActivity.class);
                } else if (menuItem.getItemId() == R.id.menu_item_transaction) {
                    intent = new Intent(MainActivity.this, TransactionActivity.class);
                } else if (menuItem.getItemId() == R.id.menu_item_loan) {
                    intent = new Intent(MainActivity.this, LoanActivity.class);
                } else if (menuItem.getItemId() == R.id.menu_item_investment) {
                    intent = new Intent(MainActivity.this, InvestmentActivity.class);
                }
                // Không cần làm gì cho menu_item_home

                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                return true; // Trả về true nếu có sự kiện được xử lý
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_meicode) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("MeiCode")
                    .setMessage("Developed by Dao vu Lam at dao.lam.7906")
                    .setNegativeButton("Visit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(MainActivity.this, WebsiteActivity.class);
                            startActivity(intent);
                        }
                    })
                    .setPositiveButton("Invite friends", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String message = "Hey, How is everything?\nCheckout this new awesome app. it helps me manage my money stuff" +
                                    "\nhttps:meiCode.org";

                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.putExtra(Intent.EXTRA_TEXT, message);
                            intent.setType("text/plain");
                            Intent chooserIntent = Intent.createChooser(intent, "Send Message via:");
                            startActivity(chooserIntent);
                        }
                    });
            builder.show();
        } else {
            // Nếu không có menu item nào được chọn
        }

        return super.onOptionsItemSelected(item);

    }
}