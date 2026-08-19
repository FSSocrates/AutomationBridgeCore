# Keep JavascriptInterface methods so WebView can call them
-keepclassmembers class com.fssocrates.abc.ABC {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep the singleton holder
-keep class com.fssocrates.abc.ABCWebViewHolder { *; }
