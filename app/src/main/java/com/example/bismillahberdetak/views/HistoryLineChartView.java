package com.example.bismillahberdetak.views;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.bismillahberdetak.R;
import com.example.bismillahberdetak.models.Reading;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryLineChartView extends View {

    private Paint hrLinePaint;
    private Paint spo2LinePaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint backgroundPaint;
    private Paint axisPaint;

    private Path hrPath;
    private Path spo2Path;

    private List<Reading> readings;
    private static final int MAX_READINGS = 10; // Show last 10 measurements

    private float padding = 60f;
    private float bottomPadding = 80f;

    private boolean isDarkMode = false;

    public HistoryLineChartView(Context context) {
        super(context);
        init(context);
    }

    public HistoryLineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HistoryLineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        readings = new ArrayList<>();

        int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        isDarkMode = (nightMode == Configuration.UI_MODE_NIGHT_YES);

        hrLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hrLinePaint.setColor(ContextCompat.getColor(context, R.color.chart_hr_line));
        hrLinePaint.setStyle(Paint.Style.STROKE);
        hrLinePaint.setStrokeWidth(5f);
        hrLinePaint.setStrokeCap(Paint.Cap.ROUND);
        hrLinePaint.setStrokeJoin(Paint.Join.ROUND);

        spo2LinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        spo2LinePaint.setColor(ContextCompat.getColor(context, R.color.chart_spo2_line));
        spo2LinePaint.setStyle(Paint.Style.STROKE);
        spo2LinePaint.setStrokeWidth(5f);
        spo2LinePaint.setStrokeCap(Paint.Cap.ROUND);
        spo2LinePaint.setStrokeJoin(Paint.Join.ROUND);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(ContextCompat.getColor(context, R.color.chart_grid));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setAlpha(100);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(ContextCompat.getColor(context, R.color.text_primary));
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(2f);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(ContextCompat.getColor(context, R.color.card_background));
        backgroundPaint.setStyle(Paint.Style.FILL);

        hrPath = new Path();
        spo2Path = new Path();
    }

    public void setReadings(List<Reading> readings) {
        this.readings = readings;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        canvas.drawRect(0, 0, width, height, backgroundPaint);

        if (readings == null || readings.isEmpty()) {
            drawEmptyState(canvas, width, height);
            return;
        }

        drawGrid(canvas, width, height);

        drawAxes(canvas, width, height);

        drawLegend(canvas, width);

        drawLines(canvas, width, height);
    }

    private void drawEmptyState(Canvas canvas, int width, int height) {
        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("No measurements yet", width / 2f, height / 2f - 20, textPaint);

        textPaint.setTextSize(28f);
        canvas.drawText("Start measuring to see your history", width / 2f, height / 2f + 30, textPaint);
    }

    private void drawGrid(Canvas canvas, int width, int height) {
        float chartHeight = height - padding - bottomPadding;
        float chartWidth = width - 2 * padding;

        for (int i = 0; i <= 5; i++) {
            float y = padding + (chartHeight * i / 5f);
            canvas.drawLine(padding, y, width - padding, y, gridPaint);
        }

        int lineCount = Math.min(readings.size(), MAX_READINGS);
        if (lineCount > 1) {
            for (int i = 0; i < lineCount; i++) {
                float x = padding + (chartWidth * i / (lineCount - 1));
                canvas.drawLine(x, padding, x, height - bottomPadding, gridPaint);
            }
        }
    }

    private void drawAxes(Canvas canvas, int width, int height) {
        float chartHeight = height - padding - bottomPadding;

        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        int hrMin = 40;
        int hrMax = 120;
        for (int i = 0; i <= 5; i++) {
            int value = hrMin + (hrMax - hrMin) * i / 5;
            float y = height - bottomPadding - (chartHeight * i / 5f);
            canvas.drawText(String.valueOf(value), padding - 10, y + 8, textPaint);
        }

        textPaint.setTextAlign(Paint.Align.LEFT);
        int spo2Min = 85;
        int spo2Max = 100;
        for (int i = 0; i <= 5; i++) {
            int value = spo2Min + (spo2Max - spo2Min) * i / 5;
            float y = height - bottomPadding - (chartHeight * i / 5f);
            canvas.drawText(value + "%", width - padding + 10, y + 8, textPaint);
        }

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(22f);
        int displayCount = Math.min(readings.size(), MAX_READINGS);
        float chartWidth = width - 2 * padding;

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (int i = 0; i < displayCount; i++) {
            Reading reading = readings.get(i);
            float x = padding + (chartWidth * i / (displayCount - 1));
            String timeStr = timeFormat.format(new Date(reading.getTimestamp() * 1000));

            canvas.drawText(timeStr, x, height - bottomPadding + 30, textPaint);

            if (i == 0 || i == displayCount - 1) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                String dateStr = dateFormat.format(reading.getDate());
                textPaint.setTextSize(18f);
                canvas.drawText(dateStr, x, height - bottomPadding + 55, textPaint);
                textPaint.setTextSize(22f);
            }
        }

        textPaint.setTextSize(26f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        canvas.save();
        canvas.rotate(-90, 20, height / 2f);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_hr_line));
        canvas.drawText("HR (bpm)", 20, height / 2f, textPaint);
        canvas.restore();

        canvas.save();
        canvas.rotate(90, width - 20, height / 2f);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_spo2_line));
        canvas.drawText("SpO2 (%)", width - 20, height / 2f, textPaint);
        canvas.restore();

        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
    }

    private void drawLegend(Canvas canvas, int width) {
        float legendY = 30f;
        float startX = width / 2f - 100;

        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.LEFT);

        hrLinePaint.setStrokeWidth(8f);
        canvas.drawLine(startX, legendY, startX + 40, legendY, hrLinePaint);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_hr_line));
        canvas.drawText("HR", startX + 50, legendY + 8, textPaint);

        spo2LinePaint.setStrokeWidth(8f);
        canvas.drawLine(startX + 120, legendY, startX + 160, legendY, spo2LinePaint);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_spo2_line));
        canvas.drawText("SpO2", startX + 170, legendY + 8, textPaint);

        hrLinePaint.setStrokeWidth(5f);
        spo2LinePaint.setStrokeWidth(5f);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
    }

    private void drawLines(Canvas canvas, int width, int height) {
        if (readings.size() < 2) return;

        float chartHeight = height - padding - bottomPadding;
        float chartWidth = width - 2 * padding;

        int displayCount = Math.min(readings.size(), MAX_READINGS);

        int hrMin = 40;
        int hrMax = 120;
        int spo2Min = 85;
        int spo2Max = 100;

        hrPath.reset();
        spo2Path.reset();

        boolean hrFirstPoint = true;
        boolean spo2FirstPoint = true;

        for (int i = 0; i < displayCount; i++) {
            Reading reading = readings.get(i);

            float x = padding + (chartWidth * i / (displayCount - 1));

            int hr = reading.getHeartRate();
            float hrNormalized = (float)(hr - hrMin) / (hrMax - hrMin);
            float hrY = height - bottomPadding - (chartHeight * hrNormalized);

            if (hrFirstPoint) {
                hrPath.moveTo(x, hrY);
                hrFirstPoint = false;
            } else {
                hrPath.lineTo(x, hrY);
            }

            int spo2 = reading.getSpo2();
            float spo2Normalized = (float)(spo2 - spo2Min) / (spo2Max - spo2Min);
            float spo2Y = height - bottomPadding - (chartHeight * spo2Normalized);

            if (spo2FirstPoint) {
                spo2Path.moveTo(x, spo2Y);
                spo2FirstPoint = false;
            } else {
                spo2Path.lineTo(x, spo2Y);
            }

            canvas.drawCircle(x, hrY, 8f, hrLinePaint);
            canvas.drawCircle(x, spo2Y, 8f, spo2LinePaint);
        }

        canvas.drawPath(hrPath, hrLinePaint);
        canvas.drawPath(spo2Path, spo2LinePaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}