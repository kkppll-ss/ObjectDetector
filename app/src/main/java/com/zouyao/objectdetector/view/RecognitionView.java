package com.zouyao.objectdetector.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import com.zouyao.objectdetector.Recognition;


/**
 * View which draws rectangles.
 */
public class RecognitionView extends View {
    private final static String TAG = "RecognitionView";

    private final List<Recognition> recognitions = new ArrayList<>();
    private Paint rectPaint = new Paint();
    private Paint textPaint = new Paint();

    public RecognitionView(Context context) {
        super(context);
    }

    public RecognitionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        rectPaint.setColor(0xffff0000);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(20);

        textPaint.setTextSize(100);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xff00ff00);
    }

    public RecognitionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        rectPaint.setColor(0xffff0000);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(20);

        textPaint.setTextSize(100);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(0xff00ff00);
    }

    /**
     * Updates rectangles which will be drawn.
     *
     * @param recognitions recognitions to draw.
     */
    public void setRecognitions(@NonNull List<Recognition> recognitions) {
        ensureMainThread();

        this.recognitions.clear();

        this.recognitions.addAll(recognitions);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Recognition recognition : recognitions) {
            RectF location = recognition.getLocation();
            float left = location.left * getWidth();
            float right = location.right * getWidth();
            float top = location.top * getHeight();
            float bottom = location.bottom * getHeight();
            Log.i(TAG, getWidth() + "*" + getHeight() + ", "
                    + "location = (" + left + ","+top + ")("+right+","+bottom+")");
            canvas.drawRect(left, top, right, bottom, rectPaint);

            float middle = (top + bottom) / 2;
            String title = recognition.getTitle();
            Float confidence = recognition.getConfidence();
            String text = String.format("%s: (%.1f%%) ", title, confidence * 100.0f);
            canvas.drawText(text, left, middle, textPaint);
        }
    }

    private void ensureMainThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalThreadStateException("This method must be called from the main thread");
        }
    }

}
