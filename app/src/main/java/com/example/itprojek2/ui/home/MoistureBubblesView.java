package com.example.itprojek2.ui.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MoistureBubblesView extends View {
    private List<Bubble> bubbles = new ArrayList<>();
    private Random random = new Random();
    private Paint paint;
    private long lastTime;

    public MoistureBubblesView(Context context) { super(context); init(); }
    public MoistureBubblesView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        // Putih transparan biar cocok menyatu dengan ungu
        paint.setColor(Color.parseColor("#15FFFFFF")); 
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bubbles.clear();
        for (int i = 0; i < 20; i++) {
            bubbles.add(new Bubble(w, h));
        }
        lastTime = System.currentTimeMillis();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;

        for (Bubble b : bubbles) {
            b.y -= b.speed * dt;
            b.x += (float) Math.sin(b.y / 60f) * 0.8f; // Efek goyang naik (wobble)
            
            // Loop ke bawah saat sampai atas
            if (b.y + b.radius < 0) {
                b.reset(getWidth(), getHeight(), true);
            }
            canvas.drawCircle(b.x, b.y, b.radius, paint);
        }
        
        invalidate(); // Animasi terus berjalan
    }

    private class Bubble {
        float x, y, radius, speed;
        Bubble(int w, int h) { reset(w, h, false); }
        void reset(int w, int h, boolean fromBottom) {
            float d = getContext().getResources().getDisplayMetrics().density;
            x = random.nextInt(Math.max(w, 1));
            y = fromBottom ? h + 20 * d + random.nextInt(50) : random.nextInt(Math.max(h, 1));
            radius = (5 + random.nextInt(12)) * d;
            speed = (20 + random.nextInt(50)) * d;
        }
    }
}
