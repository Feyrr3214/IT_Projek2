package com.example.itprojek2.ui.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class MoistureGaugeView extends View {

    private Paint trackPaint;
    private Paint progressPaint;
    private Paint textPaint;
    private Paint subTextPaint;
    private Paint labelBgPaint;
    private Paint labelTextPaint;
    private Paint tickPaint;

    private float moisturePercent = 0f; // Mulai dari 0 untuk animasi
    private float targetMoisturePercent = 0f;
    private String statusLabel = "kering";

    private static final float START_ANGLE = 150f;
    private static final float SWEEP_ANGLE = 240f;

    private RectF arcRect;
    private float strokeWidth;
    private ValueAnimator animator;

    public MoistureGaugeView(Context context) {
        super(context);
        init();
    }

    public MoistureGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MoistureGaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null); // Untuk shadow (glowing effect)

        float density = getResources().getDisplayMetrics().density;
        strokeWidth = 20f * density;

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(Color.parseColor("#E8E5F7")); // Warna abu-abu kebiruan terang
        trackPaint.setShadowLayer(10f, 0f, 4f, Color.parseColor("#1A000000"));

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(Color.parseColor("#7B6ED6"));
        progressPaint.setShadowLayer(15f, 0f, 0f, Color.parseColor("#807B6ED6")); // Glowing purple

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(48f * density);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(Color.parseColor("#2B2B2B"));

        subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setTextSize(14f * density);
        subTextPaint.setColor(Color.parseColor("#8E8E93"));

        labelBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelBgPaint.setColor(Color.parseColor("#7B6ED6"));
        labelBgPaint.setShadowLayer(8f, 0f, 4f, Color.parseColor("#4D7B6ED6"));

        labelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelTextPaint.setColor(0xFFFFFFFF);
        labelTextPaint.setTextAlign(Paint.Align.CENTER);
        labelTextPaint.setTextSize(13f * density);
        labelTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(Color.parseColor("#D1D1D6"));
        tickPaint.setStrokeWidth(2f * density);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        arcRect = new RectF();

        // Update warna awal label sesuai nilai default (0)
        updateStatusLabel(targetMoisturePercent);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float density = getResources().getDisplayMetrics().density;
        float padding = strokeWidth + 10 * density;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - padding;

        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // Tambahkan gradient modern untuk progress paint
        int[] colors = {Color.parseColor("#9D88F2"), Color.parseColor("#5F4EBE")};
        float[] positions = {0f, 1f};
        SweepGradient gradient = new SweepGradient(cx, cy, colors, positions);
        // Putar gradient agar menyesuaikan ke angka START_ANGLE
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setRotate(START_ANGLE, cx, cy);
        gradient.setLocalMatrix(matrix);
        progressPaint.setShader(gradient);

        // Draw tick marks (behind arcs)
        drawTicks(canvas, cx, cy, radius + strokeWidth / 2, density);

        // Draw track arc
        canvas.drawArc(arcRect, START_ANGLE, SWEEP_ANGLE, false, trackPaint);

        // Draw progress arc
        float progressSweep = SWEEP_ANGLE * (moisturePercent / 100f);
        canvas.drawArc(arcRect, START_ANGLE, progressSweep, false, progressPaint);

        // Draw main % text
        String percentText = (int) moisturePercent + " %";
        float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2 - 18 * density;
        canvas.drawText(percentText, cx, textY, textPaint);

        // Draw label badge
        float labelPadH = 20 * density;
        float labelPadV = 6 * density;
        float labelWidth = labelTextPaint.measureText(statusLabel) + labelPadH * 2;
        float labelHeight = 28 * density;
        float labelTop = cy + 12 * density;
        float cornerRadius = labelHeight / 2;

        RectF labelRect = new RectF(cx - labelWidth / 2, labelTop,
                cx + labelWidth / 2, labelTop + labelHeight);
        canvas.drawRoundRect(labelRect, cornerRadius, cornerRadius, labelBgPaint);

        float labelTextY = labelTop + labelHeight / 2
                - (labelTextPaint.descent() + labelTextPaint.ascent()) / 2;
        canvas.drawText(statusLabel, cx, labelTextY, labelTextPaint);
    }

    private void drawTicks(Canvas canvas, float cx, float cy, float outerRadius, float density) {
        int numTicks = 30;
        for (int i = 0; i <= numTicks; i++) {
            double angle = Math.toRadians(START_ANGLE + (SWEEP_ANGLE * i / (double) numTicks));
            float innerRadius = outerRadius - (i % 5 == 0 ? 14 * density : 8 * density);
            float startX = (float) (cx + innerRadius * Math.cos(angle));
            float startY = (float) (cy + innerRadius * Math.sin(angle));
            float endX = (float) (cx + outerRadius * Math.cos(angle));
            float endY = (float) (cy + outerRadius * Math.sin(angle));
            canvas.drawLine(startX, startY, endX, endY, tickPaint);
        }
    }

    public void setMoisturePercent(float percent) {
        this.targetMoisturePercent = Math.max(0, Math.min(100, percent));
        updateStatusLabel(targetMoisturePercent);
        
        // Mulai animasi
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        
        animator = ValueAnimator.ofFloat(moisturePercent, targetMoisturePercent);
        animator.setDuration(1500); // 1.5 detik
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            moisturePercent = (float) animation.getAnimatedValue();
            updateStatusLabel(moisturePercent); // Tetap update label selagi animasi jalan
            invalidate();
        });
        animator.start();
    }

    private void updateStatusLabel(float percent) {
        if (percent < 30) {
            statusLabel = "kering";
            labelBgPaint.setColor(Color.parseColor("#FF6B6B")); // Merah soft
            labelBgPaint.setShadowLayer(8f, 0f, 4f, Color.parseColor("#4DFF6B6B"));
        } else if (percent < 65) {
            statusLabel = "normal";
            labelBgPaint.setColor(Color.parseColor("#4ECDC4")); // Tosca soft
            labelBgPaint.setShadowLayer(8f, 0f, 4f, Color.parseColor("#4D4ECDC4"));
        } else {
            statusLabel = "basah";
            labelBgPaint.setColor(Color.parseColor("#5C85D6")); // Biru laut soft
            labelBgPaint.setShadowLayer(8f, 0f, 4f, Color.parseColor("#4D5C85D6"));
        }
    }

    public float getMoisturePercent() {
        return targetMoisturePercent;
    }
}
