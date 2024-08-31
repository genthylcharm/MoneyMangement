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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.dvl.moneymanagement.Activity.Base.BaseActivity;
import com.dvl.moneymanagement.Adapters.ItemsAdapter;
import com.dvl.moneymanagement.DataBase.DatabaseHelper;
import com.dvl.moneymanagement.Activity.Dialog.SelectItemDialog;
import com.dvl.moneymanagement.Model.Item;
import com.dvl.moneymanagement.Model.User;
import com.dvl.moneymanagement.R;
import com.dvl.moneymanagement.databinding.ActivityShoppingBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ShoppingActivity extends BaseActivity implements ItemsAdapter.GetItem {
    private static final String TAG = "ShoppingActivity";

    private ActivityShoppingBinding binding;

    private Calendar calendar = Calendar.getInstance();
    private Item selectedItem;
    private DatabaseHelper databaseHelper;
    private AddShopping addShopping;


    @Override
    public void OnGettingItemResult(Item item) {
        Log.d(TAG, "OnGettingItemResult: item: " + item.toString());
        selectedItem = item;
        binding.invisibleItemRelLayout.setVisibility(View.VISIBLE);
        Glide.with(this)
                .asBitmap()
                .load(item.getImage_url())
                .into(binding.itemImg);
        binding.txtItemName.setText(item.getName());
        binding.edtTxtDesc.setText(item.getDescription());
    }

    private DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
            calendar.set(Calendar.YEAR, i);
            calendar.set(Calendar.MONTH, i1);
            calendar.set(Calendar.DAY_OF_MONTH, i2);
            binding.edtTxtDate.setText(new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShoppingBinding.inflate(getLayoutInflater()); // Inflate the binding
        setContentView(binding.getRoot()); // Set the content view


        binding.btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(ShoppingActivity.this, dateSetListener, calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        binding.btnPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SelectItemDialog selectItemDialog = new SelectItemDialog();
                selectItemDialog.show(getSupportFragmentManager(), "select item dialog");
            }
        });

        binding.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initAdd();
            }
        });
    }

    private void initAdd() {
        Log.d(TAG, "initAdd: started");
        if (null != selectedItem) {
            if (!binding.edtTxtPrice.getText().toString().equals("")) {
                if (!binding.edtTxtDate.getText().toString().equals("")) {
                    addShopping = new AddShopping();
                    addShopping.execute();
                } else {
                    binding.txtWarning.setVisibility(View.VISIBLE);
                    binding.txtWarning.setText("Please select a date");
                }
            } else {
                binding.txtWarning.setVisibility(View.VISIBLE);
                binding.txtWarning.setText("Please add a price");
            }
        } else {
            binding.txtWarning.setVisibility(View.VISIBLE);
            binding.txtWarning.setText("Please select an Item");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != addShopping) {
            if (!addShopping.isCancelled()) {
                addShopping.cancel(true);
            }
        }
    }

    private class AddShopping extends AsyncTask<Void, Void, Void> {
        private User loggedInUser;
        private String date;
        private double price;
        private String store;
        private String description;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Utils utils = new Utils(ShoppingActivity.this);
            loggedInUser = utils.isUserLoggedIn();
            this.date = binding.edtTxtDate.getText().toString();
            this.price = -Double.valueOf(binding.edtTxtPrice.getText().toString());
            this.store = binding.edtTxtStore.getText().toString();
            this.description = binding.edtTxtDesc.getText().toString();
            databaseHelper = new DatabaseHelper(ShoppingActivity.this);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                SQLiteDatabase db = databaseHelper.getWritableDatabase();
                ContentValues transactionValue = new ContentValues();
                transactionValue.put("amount", price);
                transactionValue.put("description", description);
                transactionValue.put("user_id", loggedInUser.get_id());
                transactionValue.put("type", "shopping");
                transactionValue.put("date", date);
                transactionValue.put("recipient", store);
                long id = db.insert("transactions", null, transactionValue);

                ContentValues shoppingValues = new ContentValues();
                shoppingValues.put("item_id", selectedItem.get_id());
                shoppingValues.put("transaction_id", id);
                shoppingValues.put("user_id", loggedInUser.get_id());
                shoppingValues.put("price", price);
                shoppingValues.put("description", description);
                shoppingValues.put("date", date);
                long shoppingId = db.insert("shopping", null, shoppingValues);
                Log.d(TAG, "doInBackground: shopping id: " + shoppingId);

                Cursor cursor = db.query("users", new String[]{"remained_amount"}, "_id=?",
                        new String[]{String.valueOf(loggedInUser.get_id())}, null, null, null);
                if (null != cursor) {
                    if (cursor.moveToFirst()) {
                        @SuppressLint("Range") double remainedAmount = cursor.getDouble(cursor.getColumnIndex("remained_amount"));
                        ContentValues amountValues = new ContentValues();
                        amountValues.put("remained_amount", remainedAmount - price);
                        int affectedRows = db.update("users", amountValues, "_id=?",
                                new String[]{String.valueOf(loggedInUser.get_id())});
                        Log.d(TAG, "doInBackground: affected rows: " + affectedRows);
                    }
                    cursor.close();
                }
                db.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(ShoppingActivity.this, selectedItem.getName() + " added successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ShoppingActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

}