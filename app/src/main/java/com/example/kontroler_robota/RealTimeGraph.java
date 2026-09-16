package com.example.kontroler_robota;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class RealTimeGraph extends View {
    private Paint tiltPaint, speedPaint, axisPaint;
    private List<Float> tiltData = new ArrayList<>();
    private List<Float> speedData = new ArrayList<>();
    private static final int MAX_DATA_POINTS = 100;

    public RealTimeGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        tiltPaint = new Paint(); tiltPaint.setColor(Color.BLACK); tiltPaint.setStrokeWidth(4); tiltPaint.setStyle(Paint.Style.STROKE);
        speedPaint = new Paint(); speedPaint.setColor(Color.parseColor("#888888")); speedPaint.setStrokeWidth(3); speedPaint.setStyle(Paint.Style.STROKE);
        axisPaint = new Paint(); axisPaint.setColor(Color.LTGRAY); axisPaint.setStrokeWidth(1);
    }

    public void addData(float tilt, float speed) {
        tiltData.add(tilt);
        speedData.add(speed);
        if (tiltData.size() > MAX_DATA_POINTS) {
            tiltData.remove(0);
            speedData.remove(0);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight(), mid = h / 2;
        canvas.drawLine(0, mid, w, mid, axisPaint);

        if (tiltData.size() < 2) return;
        float step = w / (MAX_DATA_POINTS - 1);

        for (int i = 0; i < tiltData.size() - 1; i++) {
            float x1 = i * step, x2 = (i + 1) * step;
            // SKALIRANJE: Siva linija (brzina) je sada puno izraženija
            canvas.drawLine(x1, mid - speedData.get(i) * 1.5f, x2, mid - speedData.get(i+1) * 1.5f, speedPaint);
            // SKALIRANJE: Crna linija (kut)
            canvas.drawLine(x1, mid - tiltData.get(i) * 6f, x2, mid - tiltData.get(i+1) * 6f, tiltPaint);
        }
    }
}
