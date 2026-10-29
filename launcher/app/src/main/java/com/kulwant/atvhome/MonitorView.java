package com.kulwant.atvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.io.BufferedReader;
import java.io.FileReader;

/** Welcome line with a hacker symbol, plus smooth RAM and CPU-clock LED meters. Real data only. */
final class MonitorView extends View {
    private static final String CUR_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    private static final String MAX_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq";
    private static final String WELCOME = "Welcome Pranav Sarathi";

    private static final int BG = 0xE6040A06, EDGE = 0x6639FF88;
    private static final int GREEN = 0xFF39FF88, AMBER = 0xFFFFB454;
    private static final int OFF = 0x1A39FF88, LABEL = 0xFF6FBF8F;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean running;
    private long totalMb, usedMb, freqKhz = -1, maxKhz = -1;
    private float ramShown, cpuShown, ramTarget, cpuTarget;

    MonitorView(Context c) {
        super(c);
        p.setTypeface(Typeface.MONOSPACE);
    }

    private float d(float v) { return Ui.dp(getContext(), v); }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getWindowVisibility() == View.VISIBLE) start();
    }
    @Override protected void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }
    @Override protected void onWindowVisibilityChanged(int v) {
        super.onWindowVisibilityChanged(v);
        if (v == View.VISIBLE) start(); else stop();
    }

    private void start() {
        if (running) return;
        running = true;
        ui.post(slowTick);
        ui.post(frame);
    }

    private void stop() {
        running = false;
        ui.removeCallbacks(slowTick);
        ui.removeCallbacks(frame);
    }

    private final Runnable slowTick = new Runnable() {
        @Override public void run() {
            sampleSystem();
            if (running) ui.postDelayed(this, 1000);
        }
    };

    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (!running) return;
            ramShown += (ramTarget - ramShown) * 0.1f;
            cpuShown += (cpuTarget - cpuShown) * 0.1f;
            invalidate();
            ui.postDelayed(this, 40);
        }
    };

    private void sampleSystem() {
        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            totalMb = mi.totalMem >> 20;
            usedMb = Math.max(0, (mi.totalMem - mi.availMem) >> 20);
            ramTarget = totalMb > 0 ? Math.min(1f, (float) usedMb / totalMb) : 0f;
        }
        freqKhz = readLong(CUR_FREQ);
        if (maxKhz <= 0) maxKhz = readLong(MAX_FREQ);
        if (freqKhz > maxKhz) maxKhz = freqKhz;
        cpuTarget = (freqKhz > 0 && maxKhz > 0) ? Math.min(1f, (float) freqKhz / maxKhz) : 0f;
    }

    private static long readLong(String file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String s = br.readLine();
            return s == null ? -1 : Long.parseLong(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), pad = d(12);

        r.set(0.5f, 0.5f, w - 0.5f, getHeight() - 0.5f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(BG);
        c.drawRoundRect(r, d(8), d(8), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, d(1)));
        p.setColor(EDGE);
        c.drawRoundRect(r, d(8), d(8), p);
        p.setStyle(Paint.Style.FILL);

        // header: hooded hacker + welcome
        hacker(c, pad + d(10), d(19), d(20));
        p.setTextSize(d(13));
        p.setTextAlign(Paint.Align.LEFT);
        p.setColor(GREEN);
        c.drawText(WELCOME, pad + d(28), d(24), p);
        p.setColor(EDGE);
        c.drawRect(pad, d(34), w - pad, d(34) + Math.max(1f, d(0.7f)), p);

        String ramVal = totalMb > 0 ? Math.round(ramTarget * 100) + "% " + usedMb + "/" + totalMb + "M" : "n/a";
        String cpuVal = freqKhz > 0 ? (freqKhz / 1000) + "MHz" : "n/a";
        row(c, "RAM", ramShown, ramVal, d(56), w, pad);
        row(c, "CPU", cpuShown, cpuVal, d(78), w, pad);
    }

    /** Hooded figure: green hood, dark face, two glowing eyes. Centered at (cx, cy), size s. */
    private void hacker(Canvas c, float cx, float cy, float s) {
        path.reset();
        path.moveTo(cx - s * 0.5f, cy + s * 0.5f);
        path.lineTo(cx - s * 0.5f, cy);
        path.arcTo(new RectF(cx - s * 0.5f, cy - s * 0.5f, cx + s * 0.5f, cy + s * 0.5f), 180f, 180f);
        path.lineTo(cx + s * 0.5f, cy + s * 0.5f);
        path.close();
        p.setColor(GREEN);
        c.drawPath(path, p);
        p.setColor(BG);
        r.set(cx - s * 0.30f, cy - s * 0.24f, cx + s * 0.30f, cy + s * 0.34f);
        c.drawOval(r, p);
        p.setColor(GREEN);
        c.drawCircle(cx - s * 0.12f, cy + s * 0.02f, s * 0.065f, p);
        c.drawCircle(cx + s * 0.12f, cy + s * 0.02f, s * 0.065f, p);
    }

    private void row(Canvas c, String label, float frac, String value, float baseline, float w, float pad) {
        p.setTextSize(d(11));
        p.setTextAlign(Paint.Align.LEFT);
        p.setColor(LABEL);
        c.drawText(label, pad, baseline, p);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setColor(GREEN);
        c.drawText(value, w - pad, baseline, p);

        float x0 = pad + d(34), x1 = w - pad - d(92), top = baseline - d(8);
        int n = 24;
        float gap = d(2), sw = (x1 - x0 - gap * (n - 1)) / n;
        int lit = Math.round(Math.max(0f, Math.min(1f, frac)) * n);
        for (int i = 0; i < n; i++) {
            p.setColor(i < lit ? (i >= n * 0.8f ? AMBER : GREEN) : OFF);
            float x = x0 + i * (sw + gap);
            r.set(x, top, x + sw, top + d(8));
            c.drawRect(r, p);
        }
    }
}
