package com.daily.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化 TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(Locale.US);
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
                ttsReady = true;
            }
        });

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // 注册 JavaScript 接口（命名类，更稳定）
        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");

        // WebViewClient：处理外链
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }
        });

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.loadUrl("file:///android_asset/index.html");
    }

    /**
     * 统一处理 URL：
     * - file:// http:// https:// 交给 WebView 处理
     * - 其他 scheme 交给系统；没装 App 就跳浏览器搜索
     */
    private boolean handleUrl(String url) {
        if (url == null) return true;

        // 本地文件和网页走 WebView
        if (url.startsWith("file://") || url.startsWith("http://") || url.startsWith("https://")) {
            return false;
        }

        // 其他 scheme（自定义协议）：尝试打开 App
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PackageManager pm = getPackageManager();
            if (intent.resolveActivity(pm) != null) {
                startActivity(intent);
            } else {
                openBrowser(url);
            }
        } catch (Exception e) {
            openBrowser(url);
        }
        return true;
    }

    private void openBrowser(String originalUrl) {
        try {
            String query = Uri.encode(originalUrl);
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.bing.com/search?q=" + query));
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(webIntent);
        } catch (Exception ignored) {
        }
    }

    /**
     * JavaScript 调用的桥接类。命名类，比匿名内部类更稳定。
     */
    private class Bridge {
        @JavascriptInterface
        public boolean speak(String text, String lang) {
            if (tts == null || !ttsReady || text == null || text.length() == 0) return false;
            try {
                Locale locale;
                if (lang != null && lang.startsWith("zh")) {
                    locale = Locale.CHINA;
                } else if (lang != null && lang.startsWith("en-GB")) {
                    locale = Locale.UK;
                } else {
                    locale = Locale.US;
                }
                tts.setLanguage(locale);
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "daily");
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void stopSpeak() {
            try {
                if (tts != null) tts.stop();
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public boolean isReady() {
            return ttsReady;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}