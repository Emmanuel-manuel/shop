package emm.sys;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {

    private List<BarData> barDataList = new ArrayList<>();
    private Paint barPaint;
    private Paint textPaint;
    private Paint axisPaint;
    private float maxValue = 0;
    private int barColor = Color.parseColor("#4361EE");
    private int textColor = Color.BLACK;
    private int axisColor = Color.GRAY;

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint();
        barPaint.setColor(barColor);
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);

        axisPaint = new Paint();
        axisPaint.setColor(axisColor);
        axisPaint.setStrokeWidth(2);
    }

    public void setData(List<BarData> data) {
        this.barDataList = data;
        maxValue = 0;
        for (BarData barData : data) {
            if (barData.value > maxValue) {
                maxValue = barData.value;
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (barDataList.isEmpty()) {
            return;
        }

        int width = getWidth();
        int height = getHeight();
        int padding = 50;
        int chartWidth = width - 2 * padding;
        int chartHeight = height - 2 * padding;

        // Draw Y-axis
        canvas.drawLine(padding, padding, padding, height - padding, axisPaint);

        // Draw X-axis
        canvas.drawLine(padding, height - padding, width - padding, height - padding, axisPaint);

        // Draw bars
        int barCount = barDataList.size();
        int barWidth = chartWidth / (barCount * 2);
        int spacing = barWidth / 2;

        for (int i = 0; i < barCount; i++) {
            BarData data = barDataList.get(i);

            // Calculate bar dimensions
            float barHeight = (data.value / maxValue) * chartHeight;
            int left = padding + i * (barWidth + spacing) + spacing;
            int right = left + barWidth;
            int bottom = height - padding;
            int top = bottom - (int) barHeight;

            // Draw bar
            barPaint.setColor(data.color);
            canvas.drawRect(left, top, right, bottom, barPaint);

            // Draw label
            canvas.drawText(data.label, left + barWidth / 2, bottom + 40, textPaint);

            // Draw value
            canvas.drawText(String.valueOf((int) data.value), left + barWidth / 2, top - 10, textPaint);
        }
    }

    public static class BarData {
        public String label;
        public float value;
        public int color;

        public BarData(String label, float value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }
}