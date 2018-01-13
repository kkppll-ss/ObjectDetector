package com.zouyao.objectdetector;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zouyao on 1/13/18.
 */

public class Utils {
    private static final String TAG = "Utils";
    public static int getRotation(int frameRotation){
        int rotation = 0;
        switch (frameRotation)
        {
            case 270:
                rotation = 90;
                break;
            case 180:
                rotation = 180;
                break;
            case 90:
                rotation = 270;
                break;
        }
        return rotation;
    }
    private static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation) {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                Log.w(TAG, "Rotation of " + applyRotation +" % 90 != 0");
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcHeight : srcWidth;
        final int inHeight = transpose ? srcWidth : srcHeight;

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;
            final float scaleFactor = Math.min(scaleFactorX, scaleFactorY);
            matrix.postScale(scaleFactor, scaleFactor);
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }

    public static void drawRecognitions(
            Canvas canvas, List<Recognition> recognitions, Paint rectPaint,
            Paint textPaint, float textSize){
        for (Recognition recognition : recognitions) {
            RectF location = recognition.getLocation();
            float left = location.left * canvas.getWidth();
            float right = location.right * canvas.getWidth();
            float top = location.top * canvas.getHeight();
            float bottom = location.bottom * canvas.getHeight();
            Log.i(TAG, canvas.getWidth() + "*" + canvas.getHeight() + ", "
                    + "location = (" + left + "," + top + ")(" + right + "," + bottom + ")");
            canvas.drawRect(left, top, right, bottom, rectPaint);

            float middle = (left + right) / 2;
            String title = recognition.getTitle();
            Float confidence = recognition.getConfidence();
            String text = String.format("%s: (%.1f%%) ", title, confidence * 100.0f);
            canvas.drawText(text, middle, top + textSize, textPaint);
        }
    }


    public static synchronized List<Recognition>
    getRecognitionResult (ObjectDetector objectDetector, Bitmap image,
                          int frameRotation, float minimumConfidence){
        int inputSize = ObjectDetector.INPUT_SIZE;
        int rotation = getRotation(frameRotation);
        Matrix matrix = getTransformationMatrix(
                image.getWidth(), image.getHeight(),
                inputSize, inputSize, rotation);
        Bitmap resizedImage = Bitmap.createBitmap(image, 0, 0,
                image.getWidth(), image.getHeight(), matrix, false);

        int inWidth = resizedImage.getWidth();
        int inHeight = resizedImage.getHeight();


        Bitmap recognitionImage = Bitmap.createBitmap(
                inputSize, inputSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(recognitionImage);
        canvas.drawARGB(255, 255, 255, 255);
        canvas.drawBitmap(resizedImage, 0, 0, null);
        final List<Recognition> recognitions = objectDetector.recognizeImage(recognitionImage);

        final List<Recognition> results = new ArrayList<>();
        for (Recognition recognition : recognitions)
        {
            if (recognition.getConfidence() >= minimumConfidence) {
                RectF location = recognition.getLocation();
                float left = location.left / inWidth;
                float right = location.right / inWidth;
                float top = location.top / inHeight;
                float bottom = location.bottom / inHeight;
                results.add(new Recognition(
                        recognition.getId(),
                        recognition.getTitle(),
                        recognition.getConfidence(),
                        new RectF(left, top, right, bottom)
                ));
            }
        }
        return results;
    }

    public static float setAttributes(Context context, Paint rectPaint, Paint textPaint){
        Resources resources= context.getResources();

        rectPaint.setColor(resources.getColor(R.color.colorRect));
        rectPaint.setStrokeWidth(resources.getDimensionPixelSize(R.dimen.rect_stroke_size));
        rectPaint.setStyle(Paint.Style.STROKE);

        float textSize = resources.getDimensionPixelSize(R.dimen.text_size);
        textPaint.setTextSize(textSize);
        textPaint.setColor(resources.getColor(R.color.colorText));
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);

        return textSize;
    }
}
