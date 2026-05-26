package com.example.itprojek2.ui.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * MoistureChartView
 *
 * Custom View yang menggambar grafik kelembaban bergaya weather forecast:
 * - Kurva Bezier cubic yang mulus
 * - Area gradient semi-transparan di bawah kurva
 * - Titik bulat bersinar di setiap data point
 * - Label sumbu X (jam/hari/tanggal)
 * - Label nilai % di atas titik tertinggi dan terendah
 * - Animasi draw saat data dimuat
 */
public class MoistureChartView extends View {

    // ── Data ─────────────────────────────────────────────────────────────
    private List<String> labels = new ArrayList<>();
    private List<Float>  values = new ArrayList<>();
    private List<Float>  animValues = new ArrayList<>(); // nilai saat animasi berjalan

    // ── Paint ────────────────────────────────────────────────────────────
    private Paint linePaint;
    private Paint fillPaint;
    private Paint dotPaint;
    private Paint dotInnerPaint;
    private Paint labelPaint;
    private Paint valuePaint;
    private Paint gridPaint;
    private Paint noDataPaint;

    private Paint valueBgPaint; // Paint untuk background pill nilai

    // ── Animasi ──────────────────────────────────────────────────────────
    private ValueAnimator animator;
    private float animProgress = 1f; // 0 = mulai, 1 = selesai

    // ── Warna (disesuaikan dengan tema app) ──────────────────────────────
    private static final int COLOR_LINE        = Color.parseColor("#FFFFFF");
    private static final int COLOR_FILL_TOP    = Color.parseColor("#60FFFFFF");
    private static final int COLOR_FILL_BOTTOM = Color.parseColor("#00FFFFFF");
    private static final int COLOR_DOT_OUTER   = Color.parseColor("#FFFFFF");
    private static final int COLOR_DOT_INNER   = Color.parseColor("#B4AFEE");
    private static final int COLOR_LABEL       = Color.parseColor("#CCE8E6FF");
    private static final int COLOR_VALUE       = Color.parseColor("#FFFFFFFF");
    private static final int COLOR_GRID        = Color.parseColor("#20FFFFFF");
    private static final int COLOR_HIGHLIGHT   = Color.parseColor("#FFE17D"); // kuning untuk titik tertinggi

    public MoistureChartView(Context context) {
        super(context); init();
    }
    public MoistureChartView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }
    public MoistureChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        float d = getResources().getDisplayMetrics().density;

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f * d); // Sedikit lebih tebal
        linePaint.setColor(COLOR_LINE);
        linePaint.setShadowLayer(10f, 0f, 0f, Color.parseColor("#99FFFFFF"));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(COLOR_DOT_OUTER);
        dotPaint.setShadowLayer(12f, 0f, 0f, Color.parseColor("#AAFFFFFF"));

        dotInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotInnerPaint.setStyle(Paint.Style.FILL);
        dotInnerPaint.setColor(COLOR_DOT_INNER);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(COLOR_LABEL);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(10f * d);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(COLOR_VALUE);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(9.5f * d);
        valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f * d);
        gridPaint.setColor(COLOR_GRID);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{10f * d, 10f * d}, 0)); // Garis putus-putus

        valueBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valueBgPaint.setColor(Color.parseColor("#40000000")); // Hitam transparan untuk background nilai

        noDataPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        noDataPaint.setColor(Color.parseColor("#80FFFFFF"));
        noDataPaint.setTextAlign(Paint.Align.CENTER);
        noDataPaint.setTextSize(12f * d);
    }

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Set data grafik dan mulai animasi draw.
     *
     * @param newLabels label sumbu X
     * @param newValues nilai kelembaban (0-100); nilai 0 = tidak ada data
     */
    public void setData(List<String> newLabels, List<Float> newValues) {
        this.labels = newLabels != null ? newLabels : new ArrayList<>();
        this.values = newValues != null ? newValues : new ArrayList<>();

        // Init animValues ke 0 agar animasi naik dari bawah
        animValues = new ArrayList<>();
        for (int i = 0; i < this.values.size(); i++) animValues.add(0f);

        // Animasi draw
        if (animator != null && animator.isRunning()) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(900);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            animProgress = (float) a.getAnimatedValue();
            for (int i = 0; i < values.size(); i++) {
                animValues.set(i, values.get(i) * animProgress);
            }
            invalidate();
        });
        animator.start();
    }

    // ── onDraw ──────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float d  = getResources().getDisplayMetrics().density;
        int  w   = getWidth();
        int  h   = getHeight();

        float paddingLeft   = 16f * d;
        float paddingRight  = 16f * d;
        float paddingTop    = 28f * d;  // ruang untuk label nilai di atas
        float paddingBottom = 28f * d;  // ruang untuk label sumbu X di bawah

        float chartW = w - paddingLeft - paddingRight;
        float chartH = h - paddingTop - paddingBottom;
        float chartBottom = h - paddingBottom;

        if (animValues.isEmpty()) {
            // Tampilkan pesan jika tidak ada data
            canvas.drawText("Belum ada data untuk periode ini",
                    w / 2f, h / 2f, noDataPaint);
            return;
        }

        int count = animValues.size();

        // Tentukan max/min untuk scaling (min 0, max setidaknya 100)
        float maxVal = 100f;
        float minVal = 0f;
        for (float v : animValues) {
            if (v > 0) { // 0 = tidak ada data, skip
                if (v < minVal || minVal == 0) minVal = v;
            }
        }
        // Beri sedikit ruang vertikal
        float range = maxVal - 0f;

        // Hitung posisi X & Y untuk setiap titik
        float[] xs = new float[count];
        float[] ys = new float[count];
        float step = (count > 1) ? chartW / (count - 1) : chartW;

        int idxMax = 0, idxMin = 0;
        float curMax = -1f, curMin = 200f;

        for (int i = 0; i < count; i++) {
            xs[i] = paddingLeft + i * step;
            float val = animValues.get(i);
            // Y: makin besar nilai → makin ke atas (kecilkan Y)
            ys[i] = chartBottom - (val / maxVal) * chartH;

            if (val > 0) {
                if (val > curMax) { curMax = val; idxMax = i; }
                if (val < curMin) { curMin = val; idxMin = i; }
            }
        }

        // ── 1. Grid lines horizontal (Dashed) ────────────────────────────
        int[] gridPercents = {25, 50, 75, 100};
        for (int gp : gridPercents) {
            float gy = chartBottom - (gp / maxVal) * chartH;
            Path gridPath = new Path();
            gridPath.moveTo(paddingLeft, gy);
            gridPath.lineTo(w - paddingRight, gy);
            canvas.drawPath(gridPath, gridPaint);
        }

        // ── 2. Area fill dengan gradient ─────────────────────────────────
        if (count >= 2) {
            Path fillPath = buildBezierPath(xs, ys, count);

            // Tutup path ke bawah
            fillPath.lineTo(xs[count - 1], chartBottom);
            fillPath.lineTo(xs[0], chartBottom);
            fillPath.close();

            LinearGradient gradient = new LinearGradient(
                    0, paddingTop, 0, chartBottom,
                    COLOR_FILL_TOP, COLOR_FILL_BOTTOM,
                    Shader.TileMode.CLAMP
            );
            fillPaint.setShader(gradient);
            canvas.drawPath(fillPath, fillPaint);
        }

        // ── 3. Kurva garis Bezier ─────────────────────────────────────────
        if (count >= 2) {
            Path linePath = buildBezierPath(xs, ys, count);
            canvas.drawPath(linePath, linePaint);
        } else if (count == 1) {
            canvas.drawCircle(xs[0], ys[0], 5f * d, dotPaint);
        }

        // ── 4. Titik-titik data (dengan efek fade in) ─────────────────────
        float dotR      = 4.5f * d;
        float dotRInner = 2.5f * d;
        int alphaDot = (int) (255 * animProgress);

        for (int i = 0; i < count; i++) {
            if (animValues.get(i) <= 0) continue; // Skip titik tanpa data

            boolean isMax = (i == idxMax) && curMax > 0;
            boolean isMin = (i == idxMin) && curMin < 200 && idxMin != idxMax;

            // Titik tertinggi: warna kuning emas
            if (isMax) {
                Paint dotHighlight = new Paint(dotPaint);
                dotHighlight.setColor(COLOR_HIGHLIGHT);
                dotHighlight.setAlpha(alphaDot);
                dotHighlight.setShadowLayer(14f, 0f, 0f, Color.parseColor("#AAFFE17D"));
                canvas.drawCircle(xs[i], ys[i], dotR * 1.3f, dotHighlight);
                
                Paint innerHighlight = new Paint(dotInnerPaint);
                innerHighlight.setColor(Color.parseColor("#FFF3CC"));
                innerHighlight.setAlpha(alphaDot);
                canvas.drawCircle(xs[i], ys[i], dotRInner, innerHighlight);
            } else {
                dotPaint.setAlpha(alphaDot);
                dotInnerPaint.setAlpha(alphaDot);
                canvas.drawCircle(xs[i], ys[i], dotR, dotPaint);
                canvas.drawCircle(xs[i], ys[i], dotRInner, dotInnerPaint);
            }
        }

        // ── 5. Label nilai di titik tertinggi & terendah ─────────────────
        float valLabelOffset = 18f * d; // Agak naik sedikit
        if (curMax > 0) {
            String maxText = (int) (values.get(idxMax) * animProgress) + "%";
            Paint pMax = new Paint(valuePaint);
            pMax.setColor(COLOR_HIGHLIGHT);
            pMax.setAlpha(alphaDot);
            
            // Draw background pill
            float textW = pMax.measureText(maxText);
            float bgPadX = 8f * d, bgPadY = 4f * d;
            RectF bgRect = new RectF(
                    xs[idxMax] - textW/2f - bgPadX, 
                    ys[idxMax] - valLabelOffset - pMax.getTextSize() + bgPadY,
                    xs[idxMax] + textW/2f + bgPadX, 
                    ys[idxMax] - valLabelOffset + bgPadY*1.5f);
            valueBgPaint.setAlpha((int)(alphaDot * 0.4f));
            canvas.drawRoundRect(bgRect, 8f*d, 8f*d, valueBgPaint);
            
            canvas.drawText(maxText, xs[idxMax], ys[idxMax] - valLabelOffset, pMax);
        }
        if (curMin < 200 && idxMin != idxMax && curMin > 0) {
            String minText = (int) (values.get(idxMin) * animProgress) + "%";
            valuePaint.setAlpha(alphaDot);
            
            // Draw background pill
            float textW = valuePaint.measureText(minText);
            float bgPadX = 8f * d, bgPadY = 4f * d;
            RectF bgRect = new RectF(
                    xs[idxMin] - textW/2f - bgPadX, 
                    ys[idxMin] - valLabelOffset - valuePaint.getTextSize() + bgPadY,
                    xs[idxMin] + textW/2f + bgPadX, 
                    ys[idxMin] - valLabelOffset + bgPadY*1.5f);
            valueBgPaint.setAlpha((int)(alphaDot * 0.4f));
            canvas.drawRoundRect(bgRect, 8f*d, 8f*d, valueBgPaint);
            
            canvas.drawText(minText, xs[idxMin], ys[idxMin] - valLabelOffset, valuePaint);
        }

        // ── 6. Label sumbu X ─────────────────────────────────────────────
        int maxVisibleLabels = (int) (chartW / (22f * d)); // hindari tumpukan
        int step2 = Math.max(1, count / Math.max(maxVisibleLabels, 1));

        for (int i = 0; i < count; i += step2) {
            if (i >= labels.size()) break;
            float labelY = h - (paddingBottom / 2f)
                    - (labelPaint.descent() + labelPaint.ascent()) / 2f;
            canvas.drawText(labels.get(i), xs[i], labelY, labelPaint);
        }
        // Selalu tampilkan label terakhir
        if (count > 0 && (count - 1) % step2 != 0 && count - 1 < labels.size()) {
            float labelY = h - (paddingBottom / 2f)
                    - (labelPaint.descent() + labelPaint.ascent()) / 2f;
            canvas.drawText(labels.get(count - 1), xs[count - 1], labelY, labelPaint);
        }
    }

    // ── Helper: Bezier Path ──────────────────────────────────────────────

    /**
     * Bangun Path kurva Bezier cubic yang halus dari array titik xs/ys.
     * Control point dihitung otomatis agar kurva mulus di setiap titik.
     */
    private Path buildBezierPath(float[] xs, float[] ys, int count) {
        Path path = new Path();
        path.moveTo(xs[0], ys[0]);

        for (int i = 1; i < count; i++) {
            // Control point: sepertiga jarak antar titik
            float cp1x = xs[i - 1] + (xs[i] - xs[i - 1]) / 3f;
            float cp1y = ys[i - 1];
            float cp2x = xs[i] - (xs[i] - xs[i - 1]) / 3f;
            float cp2y = ys[i];
            path.cubicTo(cp1x, cp1y, cp2x, cp2y, xs[i], ys[i]);
        }
        return path;
    }
}
