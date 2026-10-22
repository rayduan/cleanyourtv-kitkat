package com.gathin.tvhome.legacy;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

/** Narrow Home redirect for vendor firmware that hardwires the factory launcher. */
public class HomeRedirectService extends AccessibilityService {
    private long lastRedirect;
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        String pkg = String.valueOf(event.getPackageName());
        String cls = String.valueOf(event.getClassName());
        boolean factoryHome = "com.bestv.ott".equals(pkg)
            && "com.bestv.ott.home.HomeActivity".equals(cls);
        boolean fallbackHome = "com.android.launcher3".equals(pkg)
            && "com.android.launcher3.Launcher".equals(cls);
        if (!factoryHome && !fallbackHome) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastRedirect < 250) return;
        lastRedirect = now;
        startActivity(new Intent(this, LegacyHomeActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }
    public void onInterrupt() { }
}
