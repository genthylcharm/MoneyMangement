package com.dvl.moneymanagement.DataBase;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final String DB_NAME = "Money Management";
    private static final int DB_VERSION = 1;
    public DatabaseHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG, "onCreate: started");
        String createUserTable = "CREATE TABLE users (_id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT NOT NULL, " +
                "password TEXT NOT NULL, " +
                "first_name TEXT, last_name TEXT, address TEXT, image_url TEXT, remained_amount DOUBLE)";

        String createShoppingTable = "CREATE TABLE shopping (_id INTEGER PRIMARY KEY AUTOINCREMENT, item_id INTEGER, " +
                "user_id INTEGER, transaction_id INTEGER, price DOUBLE, date DATE, description TEXT)";


        String createInvestmentTable = "CREATE TABLE investments (_id INTEGER PRIMARY KEY AUTOINCREMENT, amount DOUBLE, " +
                "monthly_roi DOUBLE, name TEXT, init_date DATE, finish_date DATE, user_id INTEGER, transaction_id INTEGER)";

        String createLoansTable  = "CREATE TABLE loans (_id INTEGER PRIMARY KEY AUTOINCREMENT, init_date DATE, " +
                "finish_date DATE, init_amount DOUBLE, remained_amount DOUBLE, monthly_payment DOUBLE, monthly_roi DOUBLE," +
                "name TEXT, user_id INTEGER, transaction_id INTEGER)";

        String createTransactionTable = "CREATE TABLE transactions (_id INTEGER PRIMARY KEY AUTOINCREMENT, amount double, " +
                "date DATE, type TEXT, user_id INTEGER, recipient TEXT, description TEXT)";

        String createItemsTable = "CREATE TABLE items (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, image_url TEXT," +
                "description TEXT)";

        sqLiteDatabase.execSQL(createUserTable);
        sqLiteDatabase.execSQL(createShoppingTable);
        sqLiteDatabase.execSQL(createInvestmentTable);
        sqLiteDatabase.execSQL(createLoansTable);
        sqLiteDatabase.execSQL(createTransactionTable);
        sqLiteDatabase.execSQL(createItemsTable);

        addInitialItems(sqLiteDatabase);
        addTestTransaction(sqLiteDatabase);
        addTestProfit(sqLiteDatabase);
        addTestShopping(sqLiteDatabase);
    }
    private void addTestShopping(SQLiteDatabase db) {
        Log.d(TAG, "addTestShopping: started");
        ContentValues firstValues = new ContentValues();
        firstValues.put("item_id", 1);
        firstValues.put("transaction_id", 1);
        firstValues.put("user_id", 1);
        firstValues.put("price", 10.0);
        firstValues.put("description", "some description");
        firstValues.put("date", "2019-10-01");
        db.insert("shopping", null, firstValues);

        ContentValues secondValues = new ContentValues();
        secondValues.put("item_id", 2);
        secondValues.put("transaction_id", 2);
        secondValues.put("user_id", 1);
        secondValues.put("price", 8.0);
        secondValues.put("description", "second description");
        secondValues.put("date", "2019-10-01");
        db.insert("shopping", null, secondValues);

        ContentValues thirdValues = new ContentValues();
        thirdValues.put("item_id", 2);
        thirdValues.put("transaction_id", 3);
        thirdValues.put("user_id", 1);
        thirdValues.put("price", 16.0);
        thirdValues.put("description", "third description");
        thirdValues.put("date", "2019-10-02");
        db.insert("shopping", null, thirdValues);
    }
    private void addTestProfit(SQLiteDatabase db) {
        Log.d(TAG, "addTestProfit: started");
        ContentValues firstValues = new ContentValues();
        firstValues.put("amount", 15.0);
        firstValues.put("type", "profit");
        firstValues.put("date", "2019-08-10");
        firstValues.put("description", "monthly profit from Bank of America");
        firstValues.put("user_id", 1);
        firstValues.put("recipient", "Bank of America");
        db.insert("transactions", null, firstValues);

        ContentValues secondValues = new ContentValues();
        secondValues.put("amount", 25.0);
        secondValues.put("type", "profit");
        secondValues.put("date", "2019-08-26");
        secondValues.put("description", "monthly profit from Real Estate investment");
        secondValues.put("user_id", 1);
        secondValues.put("recipient", "Real Estate Agency");
        db.insert("transactions", null, secondValues);

        ContentValues thirdValues = new ContentValues();
        thirdValues.put("amount", 32.0);
        thirdValues.put("type", "profit");
        thirdValues.put("date", "2019-07-11");
        thirdValues.put("description", "monthly profit stocks");
        thirdValues.put("user_id", 1);
        thirdValues.put("recipient", "Vangaurd");
        db.insert("transactions", null, thirdValues);
    }
    private void addTestTransaction(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG, "addTestTransaction: started");
        ContentValues values = new ContentValues();
        values.put("_id", 0);
        values.put("amount", 10.5);
        values.put("date", "2019-10-04");
        values.put("type", "shopping");
        values.put("user_id", 1);
        values.put("description", "Grocery shopping");
        values.put("recipient", "Walmart");
        long newTransactionId = sqLiteDatabase.insert("transactions", null, values);
        Log.d(TAG, "addTestTransaction: transaction id: " + newTransactionId);
    }
    private void addInitialItems (SQLiteDatabase db) {
        Log.d(TAG, "addInitialItems: started");
        ContentValues values = new ContentValues();
        values.put("name", "Bike");
        values.put("image_url", "https://cdn.shopify.com/s/files/1/0903/4494/products/Smashing-Pumpkin-GX-Eagle-complete-front-white.jpg");
        values.put("description", "The perfect mountain bike");

        db.insert("items", null, values);
    }
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
