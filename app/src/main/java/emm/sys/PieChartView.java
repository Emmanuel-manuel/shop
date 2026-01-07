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

public class PieChartView extends View {

    private List<PieData> pieDataList = new ArrayList<>();
    private Paint slicePaint;
    private Paint textPaint;
    private float totalValue = 0;
    private int[] colors = {
            Color.parseColor("#4361EE"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#F44336"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#8BC34A"),
            Color.parseColor("#FF5722")
    };

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        slicePaint = new Paint();
        slicePaint.setStyle(Paint.Style.FILL);
        slicePaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<PieData> data) {
        this.pieDataList = data;
        totalValue = 0;
        for (PieData pieData : data) {
            totalValue += pieData.value;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (pieDataList.isEmpty() || totalValue == 0) {
            return;
        }

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height) - 100;
        int left = (width - size) / 2;
        int top = (height - size) / 2;
        int right = left + size;
        int bottom = top + size;

        RectF rect = new RectF(left, top, right, bottom);
        float startAngle = 0;

        for (int i = 0; i < pieDataList.size(); i++) {
            PieData data = pieDataList.get(i);
            float sweepAngle = (data.value / totalValue) * 360;

            slicePaint.setColor(colors[i % colors.length]);
            canvas.drawArc(rect, startAngle, sweepAngle, true, slicePaint);

            // Draw label at the center of each slice
            float angle = startAngle + sweepAngle / 2;
            float radius = size / 2;
            float x = (float) (width / 2 + radius * 0.6 * Math.cos(Math.toRadians(angle)));
            float y = (float) (height / 2 + radius * 0.6 * Math.sin(Math.toRadians(angle)));

            textPaint.setColor(getContrastColor(colors[i % colors.length]));
            canvas.drawText(data.label.substring(0, Math.min(3, data.label.length())), x, y, textPaint);

            startAngle += sweepAngle;
        }
    }

    private int getContrastColor(int color) {
        double brightness = Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114;
        return brightness > 128 ? Color.BLACK : Color.WHITE;
    }

    public static class PieData {
        public String label;
        public float value;

        public PieData(String label, float value) {
            this.label = label;
            this.value = value;
        }
    }
}