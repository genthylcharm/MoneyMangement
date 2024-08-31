package com.dvl.moneymanagement.Activity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.dvl.moneymanagement.Activity.Base.BaseActivity;
import com.dvl.moneymanagement.Adapters.InvestmentAdapter;
import com.dvl.moneymanagement.DataBase.DatabaseHelper;
import com.dvl.moneymanagement.Model.Investment;
import com.dvl.moneymanagement.Model.User;
import com.dvl.moneymanagement.R;
import com.dvl.moneymanagement.databinding.ActivityInvestmentBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class InvestmentActivity extends BaseActivity {

    private static final String TAG = "InvestmentActivity";

    private ActivityInvestmentBinding binding;

    private InvestmentAdapter adapter;

    private DatabaseHelper databaseHelper;

    private GetInvestments getInvestments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInvestmentBinding.inflate(getLayoutInflater()); // Inflate binding
        setContentView(binding.getRoot()); // Set content view to the root of the binding
        initBottomNavView();
        adapter = new InvestmentAdapter(this);

        binding.investmentRecView.setAdapter(adapter); // Use binding to set the adapter
        binding.investmentRecView.setLayoutManager(new LinearLayoutManager(this)); // Use binding for RecyclerView

        databaseHelper = new DatabaseHelper(this);

        getInvestments = new GetInvestments();
        Utils utils = new Utils(this);
        User user = utils.isUserLoggedIn();

        if (null != user) {
            getInvestments.execute(user.get_id());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != getInvestments) {
            if (!getInvestments.isCancelled()) {
                getInvestments.cancel(true);
            }
        }
    }

    private class GetInvestments extends AsyncTask<Integer, Void, ArrayList<Investment>> {
        @SuppressLint("Range")
        @Override
        protected ArrayList<Investment> doInBackground(Integer... integers) {
            try {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor = db.query("investments", null, "user_id=?",
                        new String[]{String.valueOf(integers[0])}, null, null, "init_date DESC");
                if (null != cursor) {
                    if (cursor.moveToFirst()) {
                        ArrayList<Investment> investments = new ArrayList<>();
                        for (int i = 0; i < cursor.getCount(); i++) {
                            Investment investment = new Investment();
                            investment.set_id(cursor.getInt(cursor.getColumnIndex("_id")));
                            investment.setUser_id(cursor.getInt(cursor.getColumnIndex("user_id")));
                            investment.setTransaction_id(cursor.getInt(cursor.getColumnIndex("transaction_id")));
                            investment.setAmount(cursor.getDouble(cursor.getColumnIndex("amount")));
                            investment.setFinish_date(cursor.getString(cursor.getColumnIndex("finish_date")));
                            investment.setInit_date(cursor.getString(cursor.getColumnIndex("init_date")));
                            investment.setMonthly_roi(cursor.getDouble(cursor.getColumnIndex("monthly_roi")));
                            investment.setName(cursor.getString(cursor.getColumnIndex("name")));

                            investments.add(investment);
                            cursor.moveToNext();
                        }

                        cursor.close();
                        db.close();
                        return investments;
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
        protected void onPostExecute(ArrayList<Investment> investments) {
            super.onPostExecute(investments);

            if (null != investments) {
                adapter.setInvestments(investments);
            } else {
                adapter.setInvestments(new ArrayList<Investment>());
            }
        }
    }

    private void initBottomNavView() {
        Log.d(TAG, "initBottomNavView: started");
        binding.bottomNavView.setSelectedItemId(R.id.menu_item_investment);
        binding.bottomNavView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_item_stats) {
                    Intent statsIntent = new Intent(InvestmentActivity.this, StatsActivity.class);
                    statsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(statsIntent);
                } else if (menuItem.getItemId() == R.id.menu_item_transaction) {
                    Intent transactionIntent = new Intent(InvestmentActivity.this, TransactionActivity.class);
                    transactionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(transactionIntent);
                } else if (menuItem.getItemId() == R.id.menu_item_home) {
                    Intent intent = new Intent(InvestmentActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else if (menuItem.getItemId() == R.id.menu_item_loan) {
                    Intent loanIntent = new Intent(InvestmentActivity.this, LoanActivity.class);
                    loanIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loanIntent);
                } else if (menuItem.getItemId() == R.id.menu_item_investment) {
                }

                return false;
            }
        });
    }
}
