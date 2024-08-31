package com.dvl.moneymanagement.Activity.Dialog;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.dvl.moneymanagement.Adapters.ItemsAdapter;
import com.dvl.moneymanagement.DataBase.DatabaseHelper;
import com.dvl.moneymanagement.Model.Item;
import com.dvl.moneymanagement.databinding.DialogSelectItemBinding;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class SelectItemDialog extends DialogFragment implements ItemsAdapter.GetItem{
    private static final String TAG = "SelectItemDialog";

    private ItemsAdapter.GetItem getItem;
    private DialogSelectItemBinding binding; // Khai báo biến binding

    @Override
    public void OnGettingItemResult(Item item) {
        Log.d(TAG, "OnGettingItemResult: item: " + item.toString());
        try {
            getItem = (ItemsAdapter.GetItem) getActivity();
            getItem.OnGettingItemResult(item);
            dismiss();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    private ItemsAdapter adapter;
    private DatabaseHelper databaseHelper;
    private GetAllItems getAllItems;
    private SearchForItems searchForItems;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Khởi tạo View Binding
        binding = DialogSelectItemBinding.inflate(LayoutInflater.from(getContext()));

        adapter = new ItemsAdapter(getActivity(), this);
        binding.itemsRecView.setAdapter(adapter);
        binding.itemsRecView.setLayoutManager(new LinearLayoutManager(getActivity()));

        databaseHelper = new DatabaseHelper(getActivity());

        binding.edtTxtItemName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (searchForItems != null) {
                    searchForItems.cancel(true);
                }
                searchForItems = new SearchForItems();
                searchForItems.execute(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        getAllItems = new GetAllItems();
        getAllItems.execute();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setView(binding.getRoot())
                .setTitle("Select an Item");

        return builder.create();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getAllItems != null && !getAllItems.isCancelled()) {
            getAllItems.cancel(true);
        }

        if (searchForItems != null && !searchForItems.isCancelled()) {
            searchForItems.cancel(true);
        }

        // Giải phóng binding để tránh rò rỉ bộ nhớ
        binding = null;
    }

    // Tách logic chung của việc truy vấn cơ sở dữ liệu
    @SuppressLint("Range")
    private ArrayList<Item> fetchItemsFromDatabase(String selection, String[] selectionArgs) {
        ArrayList<Item> items = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = databaseHelper.getReadableDatabase();
            cursor = db.query("items", null, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Item item = new Item();
                    item.set_id(cursor.getInt(cursor.getColumnIndex("_id")));
                    item.setName(cursor.getString(cursor.getColumnIndex("name")));
                    item.setImage_url(cursor.getString(cursor.getColumnIndex("image_url")));
                    item.setDescription(cursor.getString(cursor.getColumnIndex("description")));
                    items.add(item);
                } while (cursor.moveToNext());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return items;
    }

    // AsyncTask để lấy tất cả các mục
    private class GetAllItems extends AsyncTask<Void, Void, ArrayList<Item>> {
        @Override
        protected ArrayList<Item> doInBackground(Void... voids) {
            return fetchItemsFromDatabase(null, null);
        }

        @Override
        protected void onPostExecute(ArrayList<Item> items) {
            if (items != null) {
                adapter.setItems(items);
            } else {
                adapter.setItems(new ArrayList<Item>());
            }
        }
    }

    // AsyncTask để tìm kiếm các mục
    private class SearchForItems extends AsyncTask<String, Void, ArrayList<Item>> {
        @Override
        protected ArrayList<Item> doInBackground(String... strings) {
            String query = "%" + strings[0] + "%";
            return fetchItemsFromDatabase("name LIKE ?", new String[]{query});
        }

        @Override
        protected void onPostExecute(ArrayList<Item> items) {
            if (items != null) {
                adapter.setItems(items);
            } else {
                adapter.setItems(new ArrayList<Item>());
            }
        }
    }
}

