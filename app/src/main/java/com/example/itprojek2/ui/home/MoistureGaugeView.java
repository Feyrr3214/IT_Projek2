package com.example.itprojek2.ui.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
    private Paint glowPaint;
    private Paint numberPaint;
    private Paint unitPaint;
    private Paint labelBgPaint;
    private Paint labelTextPaint;
    private Paint dotPaint;

    private float displayPercent = 0f;
    private float targetPercent  = 0f;
    private String statusLabel   = "Kering";

    private static final float START_ANGLE = 150f;
    private static final float SWEEP_ANGLE = 240f;

    private final RectF arcRect = new RectF();
    private float sw; // stroke width
    private ValueAnimator animator;

    public MoistureGaugeView(Context context) {
        super(context); init();
    }
    public MoistureGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }
    public MoistureGaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        float d = getResources().getDisplayMetrics().density;
        sw = 20f * d;

        // Track — bright semi-transparent white (dasar arc)
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(sw);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(Color.parseColor("#65FFFFFF"));

        // Wide glow behind progress
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(sw * 2.4f);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setShadowLayer(22f, 0f, 0f, Color.parseColor("#70E8E6FF"));

        // Progress arc — shader set in onDraw
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(sw);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setShadowLayer(16f, 0f, 0f, Color.parseColor("#99E8E6FF"));

        // Big number — white
        numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numberPaint.setTextSize(62f * d);
        numberPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        numberPaint.setColor(Color.WHITE);
        numberPaint.setShadowLayer(6f, 0f, 2f, Color.parseColor("#40000000"));

        // "%" unit — elegant light purple/white
        unitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        unitPaint.setTextSize(26f * d);
        unitPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        unitPaint.setColor(Color.parseColor("#E8E6FF"));

        // Badge bg — semi-transparent white
        labelBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelBgPaint.setColor(Color.parseColor("#35FFFFFF"));

        // Badge text — white
        labelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelTextPaint.setColor(Color.WHITE);
        labelTextPaint.setTextAlign(Paint.Align.CENTER);
        labelTextPaint.setTextSize(13f * d);
        labelTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Glowing dot at arc tip
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.WHITE);
        dotPaint.setShadowLayer(18f, 0f, 0f, Color.parseColor("#DDEEFFFF"));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float d  = getResources().getDisplayMetrics().density;
        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;
        float padding = sw * 1.7f + 8f * d;
        float radius  = Math.min(cx, cy) - padding;

        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // ── 1. Track arc (dasar putih transparan) ────
        canvas.drawArc(arcRect, START_ANGLE, SWEEP_ANGLE, false, trackPaint);

        // ── 2. Progress ───────────────────────────────
        float sweep = SWEEP_ANGLE * (displayPercent / 100f);
        if (sweep > 0.5f) {

            // Glow layer (wide, very transparent)
            int[] gc = {
                Color.parseColor("#00E8E6FF"),
                Color.parseColor("#35E8E6FF"),
                Color.parseColor("#00E8E6FF")
            };
            SweepGradient gg = new SweepGradient(cx, cy, gc, null);
            Matrix gm = new Matrix();
            gm.setRotate(START_ANGLE, cx, cy);
            gg.setLocalMatrix(gm);
            glowPaint.setShader(gg);
            canvas.drawArc(arcRect, START_ANGLE, sweep, false, glowPaint);

            // Progress gradient: white → light purple → primary purple
            int[] pc = {
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#D4D0F5"),
                Color.parseColor("#B4AFEE")
            };
            SweepGradient pg = new SweepGradient(cx, cy, pc, null);
            Matrix pm = new Matrix();
            pm.setRotate(START_ANGLE, cx, cy);
            pg.setLocalMatrix(pm);
            progressPaint.setShader(pg);
            canvas.drawArc(arcRect, START_ANGLE, sweep, false, progressPaint);

            // Glowing dot at the tip of progress arc
            double endRad = Math.toRadians(START_ANGLE + sweep);
            float dotX = cx + radius * (float) Math.cos(endRad);
            float dotY = cy + radius * (float) Math.sin(endRad);
            canvas.drawCircle(dotX, dotY, sw * 0.54f, dotPaint);
        }

        // ── 3. Center text ────────────────────────────
        String num = String.valueOf((int) displayPercent);
        String unit = "%";

        numberPaint.setTextAlign(Paint.Align.LEFT);
        unitPaint.setTextAlign(Paint.Align.LEFT);

        float numW = numberPaint.measureText(num);
        float unitW = unitPaint.measureText(unit);
        float totalW = numW + unitW + 4f * d;

        float startX = cx - totalW / 2f;
        float textY = cy - (numberPaint.descent() + numberPaint.ascent()) / 2f - 8f * d;

        canvas.drawText(num, startX, textY, numberPaint);
        // Align percentage right beside the number
        canvas.drawText(unit, startX + numW + 4f * d, textY, unitPaint);

        // ── 4. Status badge ───────────────────────────
        float lw  = labelTextPaint.measureText(statusLabel) + 40f * d;
        float lh  = 28f * d;
        float lt  = cy + 26f * d;
        RectF lr  = new RectF(cx - lw / 2f, lt, cx + lw / 2f, lt + lh);
        canvas.drawRoundRect(lr, lh / 2f, lh / 2f, labelBgPaint);
        float lty = lt + lh / 2f - (labelTextPaint.descent() + labelTextPaint.ascent()) / 2f;
        canvas.drawText(statusLabel, cx, lty, labelTextPaint);
    }

    // ── Public API ────────────────────────────────────

    public void setMoisturePercent(float percent) {
        targetPercent = Math.max(0f, Math.min(100f, percent));
        updateLabel(targetPercent);

        if (animator != null && animator.isRunning()) animator.cancel();
        animator = ValueAnimator.ofFloat(displayPercent, targetPercent);
        animator.setDuration(1500);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            displayPercent = (float) a.getAnimatedValue();
            updateLabel(displayPercent);
            invalidate();
        });
        animator.start();
    }

    public float getMoisturePercent() {
        return targetPercent;
    }

    private void updateLabel(float p) {
        if      (p < 30f) statusLabel = "Kering";
        else if (p < 65f) statusLabel = "Normal";
        else              statusLabel = "Basah";
    }
}
