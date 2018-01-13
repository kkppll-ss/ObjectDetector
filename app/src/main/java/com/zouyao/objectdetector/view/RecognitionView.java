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
import com.zouyao.objectdetector.Utils;


/**
 * View which draws rectangles.
 */
public class RecognitionView extends View {
    private final static String TAG = "RecognitionView";
    private float textSize;

    private final List<Recognition> recognitions = new ArrayList<>();
    private Paint rectPaint = new Paint();
    private Paint textPaint = new Paint();

    public RecognitionView(Context context) {
        super(context);
    }

    public RecognitionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        textSize = Utils.setAttributes(context, rectPaint, textPaint);
    }

    public RecognitionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        textSize = Utils.setAttributes(context, rectPaint, textPaint);
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

        Utils.drawRecognitions(canvas, recognitions, rectPaint, textPaint, textSize);
    }

    private void ensureMainThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalThreadStateException("This method must be called from the main thread");
        }
    }

}
