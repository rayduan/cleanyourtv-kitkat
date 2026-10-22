package com.gathin.tvhome.legacy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** KitKat port of the CleanYourTV home concept. Framework API 19 only; no network or services. */
public class LegacyHomeActivity extends Activity {
    private final Handler handler = new Handler();
    private final List<App> apps = new ArrayList<App>();
    private final List<String> favorites = new ArrayList<String>();
    private TextView clock, hero;
    private Typeface font;
    private SharedPreferences prefs;
    private View firstTile;
    private AlertDialog appDialog;
    private boolean chooseFavorite;
    private String replacingPackage;
    private final Runnable tick = new Runnable() {
        public void run() {
            if (clock != null) clock.setText(new SimpleDateFormat("HH:mm\nM月d日 EEEE", Locale.CHINA).format(new Date()));
            handler.postDelayed(this, 30000);
        }
    };
    private static class App {
        final String label, pkg, activity;
        App(String name, ResolveInfo info) {
            label = name; pkg = info.activityInfo.packageName; activity = info.activityInfo.name;
        }
    }
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("legacy_home", MODE_PRIVATE);
        font = Typeface.createFromAsset(getAssets(), "outfit.ttf");
    }
    protected void onResume() {
        super.onResume();
        discover(); loadFavorites(); render();
        handler.removeCallbacks(tick); tick.run();
    }
    protected void onPause() { handler.removeCallbacksAndMessages(null); super.onPause(); }
    protected void onDestroy() {
        handler.removeCallbacks(tick);
        if (appDialog != null) appDialog.dismiss();
        super.onDestroy();
    }
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size);
        view.setTextColor(color); view.setTypeface(font); return view;
    }
    private GradientDrawable surface(boolean focus) {
        GradientDrawable d = new GradientDrawable(); d.setColor(focus ? 0xff34445c : 0xff161d29);
        d.setCornerRadius(dp(16)); d.setStroke(dp(focus ? 2 : 1), focus ? Color.WHITE : 0xff293344); return d;
    }
    private void discover() {
        apps.clear(); Set<String> seen = new HashSet<String>();
        // Merge TV and ordinary launch activities; Android 4.4 lacks getLeanbackLaunchIntentForPackage.
        for (String category : new String[]{"android.intent.category.LEANBACK_LAUNCHER", Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo r : getPackageManager().queryIntentActivities(query, 0)) {
                if (r.activityInfo == null || !r.activityInfo.exported || !r.activityInfo.enabled) continue;
                String pkg = r.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
                apps.add(new App(r.loadLabel(getPackageManager()).toString(), r));
            }
        }
        final Collator collator = Collator.getInstance(Locale.CHINA);
        Collections.sort(apps, new Comparator<App>() {
            public int compare(App a, App b) { return collator.compare(a.label, b.label); }
        });
    }
    private App find(String pkg) { for (App a : apps) if (a.pkg.equals(pkg)) return a; return null; }
    private void loadFavorites() {
        favorites.clear();
        if (prefs.contains("favorites")) {
            for (String pkg : prefs.getString("favorites", "").split("\\|"))
                if (find(pkg) != null && !favorites.contains(pkg) && favorites.size() < 4) favorites.add(pkg);
        } else {
            for (String pkg : new String[]{"com.ugreen.nasprotv", "com.xiaodianshi.tv.yst", "com.ktcp.video", "com.cibn.tv"})
                if (find(pkg) != null) favorites.add(pkg);
        }
    }
    private void saveFavorites() {
        StringBuilder value = new StringBuilder();
        for (String pkg : favorites) { if (value.length() > 0) value.append('|'); value.append(pkg); }
        prefs.edit().putString("favorites", value.toString()).apply();
    }
    private void render() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(42), dp(25), dp(42), dp(24));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xff111d30, 0xff030508}));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("CLEAN YOUR TV  /  轻简版", 15, 0xff9baac2);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        clock = text("", 20, 0xffdce5f4); clock.setGravity(Gravity.RIGHT); header.addView(clock);
        root.addView(header);
        LinearLayout headline = new LinearLayout(this); headline.setOrientation(LinearLayout.VERTICAL);
        headline.setGravity(Gravity.CENTER_VERTICAL);
        headline.addView(text("欢迎回家", 20, 0xff9baac2));
        hero = text("选择喜欢的应用", 38, Color.WHITE); headline.addView(hero);
        headline.addView(text("方向键选择 · 确认打开 · 长按确认更换常用应用", 14, 0xff8d9cb2));
        root.addView(headline, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout dock = new LinearLayout(this); dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(12), dp(12), dp(12), dp(12)); dock.setBackground(surface(false));
        int tileWidth = Math.min(dp(160), (width - dp(132)) / 4);
        int tileHeight = Math.min(dp(130), height / 3);
        for (int i = 0; i < 4; i++) {
            App a = i < favorites.size() ? find(favorites.get(i)) : null;
            View tile = tile(a, tileWidth, tileHeight, true);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(tileWidth, tileHeight);
            p.setMargins(dp(4), 0, dp(4), 0); dock.addView(tile, p);
            if (i == 0) firstTile = tile;
        }
        root.addView(dock, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, dp(20), 0, 0);
        actions.addView(button("全部应用", new Runnable() { public void run() { showApps(false); } }));
        actions.addView(button("信号源", new Runnable() { public void run() { openInput(); } }));
        actions.addView(button("网络", new Runnable() { public void run() { openSettings(Settings.ACTION_WIFI_SETTINGS); } }));
        actions.addView(button("设置", new Runnable() { public void run() { openSettings(Settings.ACTION_SETTINGS); } }));
        root.addView(actions);
        setContentView(root); firstTile.requestFocus();
    }
    private View button(final String label, final Runnable action) {
        TextView b = text(label, 17, Color.WHITE); b.setGravity(Gravity.CENTER);
        b.setPadding(dp(22), dp(10), dp(22), dp(10)); b.setFocusable(true); b.setClickable(true);
        b.setBackground(surface(false));
        b.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) { v.setBackground(surface(hasFocus)); }
        });
        b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { action.run(); } });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2); p.setMargins(dp(5), 0, dp(5), 0); b.setLayoutParams(p);
        return b;
    }
    private View tile(final App app, int width, int height, final boolean dock) {
        LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER); tile.setPadding(dp(8), dp(8), dp(8), dp(8));
        tile.setFocusable(true); tile.setClickable(true); tile.setBackground(surface(false));
        if (app != null) {
            ImageView icon = new ImageView(this);
            try {
                Drawable d = getPackageManager().getActivityIcon(new android.content.ComponentName(app.pkg, app.activity));
                int size = dp(56); Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bitmap); d.setBounds(0, 0, size, size); d.draw(c); icon.setImageBitmap(bitmap);
            } catch (Exception ignored) { icon.setImageResource(android.R.drawable.sym_def_app_icon); }
            int size = Math.min(dp(56), height / 2); tile.addView(icon, new LinearLayout.LayoutParams(size, size));
        } else tile.addView(text("＋", 36, 0xff8baddc));
        TextView name = text(app == null ? "添加常用应用" : app.label, 14, Color.WHITE);
        name.setSingleLine(true); name.setEllipsize(android.text.TextUtils.TruncateAt.END); name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(8); tile.addView(name, p);
        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean focus) {
                v.setBackground(surface(focus));
                if (focus && dock) hero.setText(app == null ? "添加常用应用" : app.label);
            }
        });
        tile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (app == null) showApps(true);
                else if (!dock && chooseFavorite) toggleFavorite(app);
                else launch(app);
            }
        });
        tile.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (app == null) showApps(true);
                else if (dock) new AlertDialog.Builder(LegacyHomeActivity.this)
                    .setTitle(app.label).setItems(new String[]{"从常用栏移除", "更换常用应用"}, new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface d, int which) {
                            if (which == 0) { favorites.remove(app.pkg); saveFavorites(); render(); tickNow(); }
                            else { showApps(true); replacingPackage = app.pkg; }
                        }
                    }).show();
                else toggleFavorite(app);
                return true;
            }
        });
        // Some KitKat TV firmwares do not translate a held D-pad centre into View long-clicks.
        // Handle both timed physical holds and the repeat/long-press flag sent by ADB/remotes.
        final boolean[] held = new boolean[]{false};
        final View target = tile;
        final Runnable hold = new Runnable() {
            public void run() {
                if (target.isFocused() && target.hasWindowFocus()) {
                    held[0] = true; target.performLongClick();
                }
            }
        };
        tile.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int key, KeyEvent e) {
                if (key != KeyEvent.KEYCODE_DPAD_CENTER && key != KeyEvent.KEYCODE_ENTER) return false;
                if (e.getAction() == KeyEvent.ACTION_DOWN) {
                    if (e.getRepeatCount() == 0) { held[0] = false; handler.postDelayed(hold, 600); }
                    else if (!held[0]) { handler.removeCallbacks(hold); held[0] = true; v.performLongClick(); }
                    return true;
                }
                if (e.getAction() == KeyEvent.ACTION_UP) {
                    handler.removeCallbacks(hold);
                    if (!held[0] && !e.isCanceled()) v.performClick();
                    return true;
                }
                return false;
            }
        });
        return tile;
    }
    private void tickNow() { if (clock != null) clock.setText(new SimpleDateFormat("HH:mm\nM月d日 EEEE", Locale.CHINA).format(new Date())); }
    private void toggleFavorite(App app) {
        if (replacingPackage != null && favorites.contains(replacingPackage)) {
            int slot = favorites.indexOf(replacingPackage);
            if (favorites.contains(app.pkg)) Collections.swap(favorites, slot, favorites.indexOf(app.pkg));
            else favorites.set(slot, app.pkg);
        } else if (favorites.contains(app.pkg)) favorites.remove(app.pkg);
        else if (favorites.size() < 4) favorites.add(app.pkg);
        else { Toast.makeText(this, "常用栏已满，请先长按常用应用移除一个", Toast.LENGTH_LONG).show(); return; }
        saveFavorites(); if (appDialog != null) appDialog.dismiss(); render(); tickNow();
    }
    private void showApps(boolean selecting) {
        chooseFavorite = selecting;
        replacingPackage = null;
        if (appDialog != null) appDialog.dismiss();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        GridLayout grid = new GridLayout(this); grid.setColumnCount(5); grid.setPadding(dp(10), dp(10), dp(10), dp(10));
        int w = (getResources().getDisplayMetrics().widthPixels - dp(140)) / 5;
        for (App a : apps) {
            View tile = tile(a, w, dp(105), false);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width = w; p.height = dp(105);
            p.setMargins(dp(2), dp(2), dp(2), dp(2)); grid.addView(tile, p);
        }
        scroll.addView(grid);
        appDialog = new AlertDialog.Builder(this).setTitle(selecting ? "选择常用应用（最多 4 个）" : "全部应用 · 长按确认加入常用栏")
            .setView(scroll).setNegativeButton("返回", null).create();
        appDialog.show(); appDialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels - dp(60), getResources().getDisplayMetrics().heightPixels - dp(50));
        if (grid.getChildCount() > 0) grid.getChildAt(0).requestFocus();
    }
    private void launch(App a) {
        if (appDialog != null) appDialog.dismiss();
        try { startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(a.pkg, a.activity)); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "应用不可用，请返回桌面刷新", Toast.LENGTH_SHORT).show(); }
        catch (SecurityException e) { Toast.makeText(this, "系统限制了此应用的启动", Toast.LENGTH_SHORT).show(); }
    }
    private boolean openPackage(String pkg) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
        if (intent == null) return false;
        try { startActivity(intent); return true; } catch (Exception e) { return false; }
    }
    private void openInput() {
        // The vendor input picker has MAIN but no LAUNCHER category, so package launch lookup fails.
        if (tryOpenSettings(new Intent(Intent.ACTION_MAIN).setClassName(
                "com.baofengtv.sourceswitch", "com.baofengtv.sourceswitch.MainActivity"))) return;
        if (tryOpenSettings(new Intent("android.settings.TV_INPUT_SETTINGS"))) return;
        openSettings(Settings.ACTION_SETTINGS);
    }
    private boolean tryOpenSettings(Intent intent) {
        try { startActivity(intent); return true; }
        catch (ActivityNotFoundException e) { return false; }
        catch (SecurityException e) { return false; }
    }
    private void openSettings(String action) {
        // BAOFENG 4.4 does not implement the standard Settings actions. Its package also
        // advertises factory/test activities, so getLaunchIntentForPackage is ambiguous.
        Intent baofeng = new Intent(Intent.ACTION_MAIN)
            .setClassName("com.baofengtv.settings", "com.baofengtv.settings.MainActivity")
            .putExtra("page", "home");
        if (Settings.ACTION_WIFI_SETTINGS.equals(action)) {
            // Verified against the vendor's ExternalBootReceiver/IntentUtils: home group 0
            // is Network and includes both Ethernet and Wi-Fi on this television.
            baofeng.putExtra("group", 0);
        }
        if (tryOpenSettings(baofeng)) return;
        if (tryOpenSettings(new Intent(action))) return;
        if (!Settings.ACTION_SETTINGS.equals(action)
                && tryOpenSettings(new Intent(Settings.ACTION_SETTINGS))) return;
        Toast.makeText(this, "此系统未提供可访问的设置入口", Toast.LENGTH_LONG).show();
    }
    public void onBackPressed() { if (firstTile != null) firstTile.requestFocus(); }
    public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_MENU) { showApps(false); return true; }
        return super.onKeyDown(key, event);
    }
}
