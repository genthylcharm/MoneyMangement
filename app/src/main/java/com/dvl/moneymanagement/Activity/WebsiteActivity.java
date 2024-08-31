package com.dvl.moneymanagement.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.dvl.moneymanagement.Activity.Base.BaseActivity;
import com.dvl.moneymanagement.R;
import com.dvl.moneymanagement.databinding.ActivityWebsiteBinding;

public class WebsiteActivity extends BaseActivity {
    private static final String TAG = "WebsiteActivity";
    private ActivityWebsiteBinding binding;
    private WebView webView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebsiteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.webView.setWebViewClient(new WebViewClient());
        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.loadUrl("https://www.facebook.com/dao.lam.7906/");
    }
    @Override
    public void onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack();
        }else {
            super.onBackPressed();
        }
    }
}