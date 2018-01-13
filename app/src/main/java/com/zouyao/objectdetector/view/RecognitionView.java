package com.zouyao.objectdetector.view;

import android.content.Context;
import android.content.res.TypedArray;
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

import com.zouyao.objectdetector.R;
import com.zouyao.objectdetector.Recognition;


/**
 * View which draws rectangles.
 */
public class RecognitionView extends View {
    private final static String TAG = "RecognitionView";
    private final static int textSize = 40;

    private final List<Recognition> recognitions = new ArrayList<>();
    private Paint rectPaint = new Paint();
    private Paint textPaint = new Paint();

    public RecognitionView(Context context) {
        super(context);
    }

    public RecognitionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        applyAttributes(context, attrs);
    }

    public RecognitionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyAttributes(context, attrs);
    }

    private void applyAttributes(Context context, @Nullable AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RecognitionView);
        rectPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);

        try {
            rectPaint.setColor(array.getColor(
                    R.styleable.RecognitionView_rectColor, 0xffff0000));
            rectPaint.setStrokeWidth(array.getDimension(
                    R.styleable.RecognitionView_rectStrokeWidth, 5));

            textPaint.setTextSize(array.getDimension(
                    R.styleable.RecognitionView_textSize, 20));

            textPaint.setColor(array.getColor(
                    R.styleable.RecognitionView_textColor, 0xff00ff00));
        }finally {
            array.recycle();
        }

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

            float middle = (left + right) / 2;
            String title = recognition.getTitle();
            Float confidence = recognition.getConfidence();
            String text = String.format("%s: (%.1f%%) ", title, confidence * 100.0f);
            canvas.drawText(text, middle, bottom - textSize, textPaint);
        }
    }

    private void ensureMainThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalThreadStateException("This method must be called from the main thread");
        }
    }

}
