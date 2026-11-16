package com.austin.lockscreen;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Phone-style lock screen for a Firestick.
 * Shows a big clock, the date, and live weather. Any remote button "unlocks"
 * by opening Projectivy Launcher.
 */
public class MainActivity extends Activity {

    // ---- Things you might want to change ----
    private static final String LAUNCHER_PACKAGE = "com.spocky.projengmenu"; // Projectivy
    private static final String CITY_NAME = "Toledo";
    private static final double LATITUDE = 41.6528;
    private static final double LONGITUDE = -83.5379;
    private static final long WEATHER_REFRESH_MS = 15 * 60 * 1000L;
    // -----------------------------------------

    private static final int TEXT_SOFT = Color.parseColor("#D8D6F0");
    private static final int PILL_BG = Color.parseColor("#26FFFFFF");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm", Locale.US);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.US);

    private TextView clockView;
    private TextView dateView;
    private TextView weatherTemp;
    private TextView weatherDesc;
    private LinearLayout weatherPill;

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            Date now = new Date();
            clockView.setText(timeFormat.format(now));
            dateView.setText(dateFormat.format(now));
            // Re-run right at the start of the next minute
            long delay = 60000 - (System.currentTimeMillis() % 60000) + 50;
            handler.postDelayed(this, delay);
        }
    };

    private final Runnable weatherTick = new Runnable() {
        @Override
        public void run() {
            fetchWeather();
            handler.postDelayed(this, WEATHER_REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(clockTick);
        handler.post(weatherTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockTick);
        handler.removeCallbacks(weatherTick);
    }

    // Any remote button unlocks. Volume and mute are left alone so they still work.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            return super.onKeyDown(keyCode, event);
        }
        unlock();
        return true;
    }

    @Override
    public void onBackPressed() {
        unlock();
    }

    private void unlock() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(LAUNCHER_PACKAGE);
        if (intent == null) {
            Toast.makeText(this, "Projectivy isn't installed", Toast.LENGTH_LONG).show();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Couldn't open Projectivy", Toast.LENGTH_LONG).show();
        }
    }

    // ---------------- UI ----------------

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.addView(new WallpaperView(this), matchParent());

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(0, dp(28), 0, 0);

        column.addView(new LockIconView(this), new LinearLayout.LayoutParams(dp(22), dp(28)));

        dateView = text(22, Typeface.create("sans-serif-medium", Typeface.NORMAL), TEXT_SOFT);
        LinearLayout.LayoutParams dateLp = wrap();
        dateLp.topMargin = dp(14);
        column.addView(dateView, dateLp);

        clockView = text(150, Typeface.create("sans-serif-medium", Typeface.BOLD), Color.WHITE);
        clockView.setIncludeFontPadding(false);
        clockView.setShadowLayer(dp(8), 0, dp(2), Color.parseColor("#40000000"));
        LinearLayout.LayoutParams clockLp = wrap();
        clockLp.topMargin = dp(4);
        column.addView(clockView, clockLp);

        weatherPill = new LinearLayout(this);
        weatherPill.setOrientation(LinearLayout.VERTICAL);
        weatherPill.setPadding(dp(22), dp(12), dp(22), dp(12));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(PILL_BG);
        pillBg.setCornerRadius(dp(20));
        weatherPill.setBackground(pillBg);
        weatherTemp = text(22, Typeface.create("sans-serif-medium", Typeface.NORMAL), Color.WHITE);
        weatherDesc = text(13, Typeface.DEFAULT, TEXT_SOFT);
        weatherTemp.setText("--°");
        weatherDesc.setText(CITY_NAME + ", loading");
        weatherPill.addView(weatherTemp, wrap());
        weatherPill.addView(weatherDesc, wrap());
        LinearLayout.LayoutParams pillLp = wrap();
        pillLp.topMargin = dp(26);
        column.addView(weatherPill, pillLp);

        root.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        // Bottom bar + hint, like the swipe-up bar on a phone
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setGravity(Gravity.CENTER_HORIZONTAL);
        View bar = new View(this);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(Color.parseColor("#CCFFFFFF"));
        barBg.setCornerRadius(dp(3));
        bar.setBackground(barBg);
        bottom.addView(bar, new LinearLayout.LayoutParams(dp(130), dp(5)));
        TextView hint = text(14, Typeface.DEFAULT, TEXT_SOFT);
        hint.setText("Press any button to open");
        LinearLayout.LayoutParams hintLp = wrap();
        hintLp.topMargin = dp(10);
        bottom.addView(hint, hintLp);
        FrameLayout.LayoutParams bottomLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        bottomLp.bottomMargin = dp(30);
        root.addView(bottom, bottomLp);

        return root;
    }

    private TextView text(float sp, Typeface face, int color) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(face);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    // ---------------- Weather (Open-Meteo, free, no API key) ----------------

    private void fetchWeather() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + LATITUDE
                            + "&longitude=" + LONGITUDE
                            + "&current=temperature_2m,weather_code&temperature_unit=fahrenheit");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) sb.append(line);
                    in.close();

                    JSONObject current = new JSONObject(sb.toString()).getJSONObject("current");
                    final long temp = Math.round(current.getDouble("temperature_2m"));
                    final String desc = describe(current.getInt("weather_code"));
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            weatherTemp.setText(temp + "°");
                            weatherDesc.setText(CITY_NAME + ", " + desc);
                        }
                    });
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (weatherTemp.getText().toString().startsWith("--")) {
                                weatherDesc.setText(CITY_NAME + ", weather unavailable");
                            }
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    // WMO weather codes used by Open-Meteo
    private static String describe(int code) {
        if (code == 0) return "clear";
        if (code <= 2) return "partly cloudy";
        if (code == 3) return "cloudy";
        if (code == 45 || code == 48) return "foggy";
        if (code >= 51 && code <= 57) return "drizzle";
        if (code >= 61 && code <= 67) return "rain";
        if (code >= 71 && code <= 77) return "snow";
        if (code >= 80 && code <= 82) return "showers";
        if (code == 85 || code == 86) return "snow showers";
        if (code >= 95) return "thunderstorms";
        return "";
    }

    // ---------------- Custom drawn views ----------------

    /** Soft overlapping circles on a dark navy background. */
    static class WallpaperView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        WallpaperView(Activity a) {
            super(a);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            c.drawColor(Color.parseColor("#1A1F3A"));
            paint.setColor(Color.parseColor("#2B2F6B"));
            c.drawOval(new RectF(-0.15f * w, 0.2f * h, 0.55f * w, 1.4f * h), paint);
            paint.setColor(Color.parseColor("#4A2F6B"));
            c.drawOval(new RectF(0.55f * w, -0.3f * h, 1.1f * w, 0.6f * h), paint);
            paint.setColor(Color.parseColor("#6B3A5C"));
            c.drawOval(new RectF(0.45f * w, 0.75f * h, 0.85f * w, 1.35f * h), paint);
        }
    }

    /** Small padlock drawn with shapes, so there's no image file to manage. */
    static class LockIconView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

        LockIconView(Activity a) {
            super(a);
            fill.setColor(Color.WHITE);
            stroke.setColor(Color.WHITE);
            stroke.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            stroke.setStrokeWidth(w * 0.14f);
            float inset = w * 0.22f;
            c.drawArc(new RectF(inset, h * 0.05f, w - inset, h * 0.65f), 180, 180, false, stroke);
            c.drawLine(inset, h * 0.35f, inset, h * 0.5f, stroke);
            c.drawLine(w - inset, h * 0.35f, w - inset, h * 0.5f, stroke);
            c.drawRoundRect(new RectF(0, h * 0.45f, w, h), w * 0.15f, w * 0.15f, fill);
        }
    }
}
