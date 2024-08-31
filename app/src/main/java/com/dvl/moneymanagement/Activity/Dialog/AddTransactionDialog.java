package com.dvl.moneymanagement.Activity.Dialog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.dvl.moneymanagement.Activity.AddInvestmentActivity;
import com.dvl.moneymanagement.Activity.AddLoanActivity;
import com.dvl.moneymanagement.Activity.TransferActivity;
import com.dvl.moneymanagement.Activity.ShoppingActivity;
import com.dvl.moneymanagement.databinding.DialogAddTransactionBinding;

import org.jetbrains.annotations.Nullable;

public class AddTransactionDialog extends DialogFragment {
    private static final String TAG = "AddTransactionDialog";

    private DialogAddTransactionBinding binding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogAddTransactionBinding.inflate(LayoutInflater.from(getContext()));

        // Thiết lập OnClickListener cho từng RelativeLayout bằng cách sử dụng phương thức setupClickListener
        setupClickListener(binding.shoppingRelLayout, ShoppingActivity.class);
        setupClickListener(binding.investmentRelLayout, AddInvestmentActivity.class);
        setupClickListener(binding.loanRelLayout, AddLoanActivity.class);
        setupClickListener(binding.transactionRelLayout, TransferActivity.class);

        // Tạo và trả về dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle("Add Transaction")
                .setNegativeButton("Dismiss", (dialogInterface, i) -> {
                    // Xử lý sự kiện khi nhấn nút "Dismiss"
                })
                .setView(binding.getRoot());

        return builder.create();
    }

    // Phương thức setupClickListener để tránh lặp lại mã
    private void setupClickListener(View layout, Class<?> activityClass) {
        layout.setOnClickListener(view -> startActivity(new Intent(getActivity(), activityClass)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Giải phóng binding khi view bị hủy
    }
}

