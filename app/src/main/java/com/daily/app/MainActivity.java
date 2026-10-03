package com.daily.app;

import android.annotation.SuppressLint;
import android.content.Intent;
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

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                ttsReady = (result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED);
                if (ttsReady) {
                    tts.setSpeechRate(0.9f);
                }
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
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new Object() {

            @JavascriptInterface
            public boolean isTtsReady() {
                return ttsReady;
            }

            @JavascriptInterface
            public boolean speak(String text, String lang) {
                if (tts == null || !ttsReady || text == null) return false;
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
            }

            @JavascriptInterface
            public void stopSpeak() {
                if (tts != null) tts.stop();
            }

            /**
             * 打开外部 App。scheme 是 App 协议，webUrl 是兜底网页。
             */
            @JavascriptInterface
            public boolean openApp(String scheme, String webUrl) {
                if (scheme == null || scheme.isEmpty()) return false;
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(scheme));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    // 没装 App，用浏览器打开兜底网页
                    if (webUrl != null && !webUrl.isEmpty()) {
                        try {
                            Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                            web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(web);
                            return true;
                        } catch (Exception ignored) {}
                    }
                    return false;
                }
            }

            @JavascriptInterface
            public void openUrl(String url) {
                if (url == null || url.isEmpty()) return;
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception ignored) {}
            }
        }, "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("file://") || url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception ignored) {}
                return true;
            }
        });

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.loadUrl("file:///android_asset/index.html");
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
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}