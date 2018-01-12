package com.zouyao.objectdetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.zouyao.objectdetector.ObjectDetector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;

/**
 * {@link FrameProcessor} which detects faces on camera frames.
 * <p>
 * Use {@link #with(Context)} to create a new instance.
 */
public class ObjectDetectorProcessor implements FrameProcessor {

    private static String TAG = "ObjectDetectorProcessor";

    private static Handler MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    private static final float MINIMUM_CONFIDENCE = 0.3f;

    private static final int INPUT_SIZE = 300;
    private static final String MODEL_FILE =
            "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    private static final String LABELS_FILE = "file:///android_asset/coco_labels_list.txt";


    private final ObjectDetector objectDetector;
    private final OnObjectsDetectedListener listener;

    private ObjectDetectorProcessor(Builder builder) throws IOException {
            objectDetector = ObjectDetector.create(
                    builder.context.getAssets(),
                    MODEL_FILE,
                    LABELS_FILE,
                    INPUT_SIZE);
        listener = builder.listener;
    }

    public static Builder with(Context context) {
        return new Builder(context);
    }

    @Override
    public void processFrame(Frame frame) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int width = frame.size.width;
        int height = frame.size.height;
        YuvImage yuvImage = new YuvImage(frame.image, ImageFormat.NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
        int rotation = 0;
        switch (frame.rotation)
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

        byte[] imageBytes = out.toByteArray();
        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Matrix matrix = getTransformationMatrix(image.getWidth(), image.getHeight(), INPUT_SIZE,
                INPUT_SIZE, rotation);
        Bitmap resizedImage = Bitmap.createBitmap(image, 0, 0,
                image.getWidth(), image.getHeight(), matrix, false);

        Bitmap recognitionImage = Bitmap.createScaledBitmap(resizedImage, INPUT_SIZE, INPUT_SIZE, false);

        Log.i(TAG, "rotation = " + rotation + " x scale = " + (double)image.getWidth() / INPUT_SIZE
        + " y scale = " + (double)image.getHeight() / INPUT_SIZE);
        final List<Recognition> faces = objectDetector.recognizeImage(recognitionImage);

        final List<Recognition> results = new ArrayList<>();
        for (Recognition face : faces)
        {
            if (face.getConfidence() >= MINIMUM_CONFIDENCE) {
                RectF location = face.getLocation();
                float left = location.left / INPUT_SIZE;
                float right = location.right / INPUT_SIZE;
                float top = location.top / INPUT_SIZE;
                float bottom = location.bottom / INPUT_SIZE;
                results.add(new Recognition(
                        face.getId(),
                        face.getTitle(),
                        face.getConfidence(),
                        new RectF(left, top, right, bottom)
                ));
            }
        }

        MAIN_THREAD_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                listener.onObjectsDetected(results);
            }
        });
    }

    public void close() {
        objectDetector.close();
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
            matrix.postScale(scaleFactorX, scaleFactorY);
        }

        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }

    /**
     * Notified when faces are detected.
     */
    public interface OnObjectsDetectedListener {

        /**
         * Null-object for {@link OnObjectsDetectedListener}.
         */
        OnObjectsDetectedListener NULL = new OnObjectsDetectedListener() {
            @Override
            public void onObjectsDetected(List<Recognition> faces) {
                // Do nothing
            }
        };

        /**
         * Called when faces are detected. Always called on the main thread.
         *
         * @param faces detected faces. If no faces were detected - an empty list.
         */
        void onObjectsDetected(List<Recognition> faces);

    }

    /**
     * Builder for {@link ObjectDetectorProcessor}.
     */
    public static class Builder {

        private final Context context;
        private OnObjectsDetectedListener listener = OnObjectsDetectedListener.NULL;

        private Builder(Context context) {
            this.context = context;
        }

        /**
         * @param listener which will be notified when faces are detected.
         */
        public Builder listener(OnObjectsDetectedListener listener) {
            this.listener = listener != null
                    ? listener
                    : OnObjectsDetectedListener.NULL;

            return this;
        }

        public ObjectDetectorProcessor build() throws IOException {
            return new ObjectDetectorProcessor(this);
        }

    }

}
