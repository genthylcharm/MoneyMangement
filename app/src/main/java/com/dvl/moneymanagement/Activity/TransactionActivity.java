package com.dvl.moneymanagement.Activity;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.dvl.moneymanagement.Activity.Base.BaseActivity;
import com.dvl.moneymanagement.Adapters.TransactionAdapter;
import com.dvl.moneymanagement.DataBase.DatabaseHelper;
import com.dvl.moneymanagement.Model.Transaction;
import com.dvl.moneymanagement.Model.User;
import com.dvl.moneymanagement.R;
import com.dvl.moneymanagement.databinding.ActivityTransactionBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class TransactionActivity extends BaseActivity {

    private static final String TAG = "TransactionActivity";

    private ActivityTransactionBinding binding; // Use the generated binding class

    private TransactionAdapter adapter;
    private DatabaseHelper databaseHelper;
    private GetTransactions getTransactions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initBottomNavView();

        adapter = new TransactionAdapter();

       binding.transactionRecView.setAdapter(adapter);
        binding.transactionRecView.setLayoutManager(new LinearLayoutManager(this));
        databaseHelper = new DatabaseHelper(this);
        initSearch();
        binding.btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initSearch();
            }
        });

        binding.rgType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                initSearch();
            }
        });
    }
    private void initSearch() {
        Log.d(TAG, "initSearch: started");

        Utils utils = new Utils(this);
        User user = utils.isUserLoggedIn();
        if (null != user) {
            getTransactions = new GetTransactions();
            getTransactions.execute(user.get_id());
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
    }

    private class GetTransactions extends AsyncTask<Integer, Void, ArrayList<Transaction>> {

        private String type = "all";
        private double min = 0.0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            this.min = Double.valueOf(binding.edtTxtMin.getText().toString());

            if (binding.rgType.getCheckedRadioButtonId() == R.id.rbInvestment) {
                type = "investment";
            } else if (binding.rgType.getCheckedRadioButtonId() == R.id.rbLoan) {
                type = "loan";
            } else if (binding.rgType.getCheckedRadioButtonId() == R.id.rbLoanPayment) {
                type = "loan_payment";
            } else if (binding.rgType.getCheckedRadioButtonId() == R.id.rbProfit) {
                type = "profit";
            } else if (binding.rgType.getCheckedRadioButtonId() == R.id.rbShopping) {
                type = "shopping";
            } else if (binding.rgType.getCheckedRadioButtonId() == R.id.rbSend) {
                type = "send";
            } else if (binding.rgType.getCheckedRadioButtonId() == R.id.rbReceive) {
                type = "receive";
            } else {
                type = "all";
            }
        }

        @SuppressLint("Range")
        @Override
        protected ArrayList<Transaction> doInBackground(Integer... integers) {

            try {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor;
                if (type.equals("all")) {
                    cursor = db.query("transactions", null, "user_id=?",
                            new String[]{String.valueOf(integers[0])}, null, null, "date DESC");
                } else {
                    cursor = db.query("transactions", null, "type=? AND user_id=?",
                            new String[]{type, String.valueOf(integers[0])}, null, null, "date DESC");
                }

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

                            double absoluteAmount = transaction.getAmount();
                            if (absoluteAmount < 0) {
                                absoluteAmount = -absoluteAmount;
                            }

                            if (absoluteAmount > this.min) {
                                transactions.add(transaction);
                            }
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
                binding.txtNoTransaction.setVisibility(View.GONE);
                adapter.setTransactions(transactions);
            } else {
                binding.txtNoTransaction.setVisibility(View.VISIBLE);
                adapter.setTransactions(new ArrayList<Transaction>());
            }
        }
    }

    private void initBottomNavView() {
        Log.d(TAG, "initBottomNavView: started");
        binding.bottomNavView.setSelectedItemId(R.id.menu_item_transaction);
        binding.bottomNavView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_item_stats) {
                    Intent statsIntent = new Intent(TransactionActivity.this, StatsActivity.class);
                    statsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(statsIntent);
                } else if (menuItem.getItemId() == R.id.menu_item_transaction) {
                    // Handle transaction action if needed
                } else if (menuItem.getItemId() == R.id.menu_item_home) {
                    Intent homeIntent = new Intent(TransactionActivity.this, MainActivity.class);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(homeIntent);
                } else if (menuItem.getItemId() == R.id.menu_item_loan) {
                    Intent loanIntent = new Intent(TransactionActivity.this, LoanActivity.class);
                    loanIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loanIntent);
                } else if (menuItem.getItemId() == R.id.menu_item_investment) {
                    Intent investmentIntent = new Intent(TransactionActivity.this, InvestmentActivity.class);
                    investmentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(investmentIntent);
                }

                return false;
            }
        });
    }
}