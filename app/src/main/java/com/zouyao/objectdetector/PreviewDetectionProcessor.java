package com.zouyao.objectdetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.util.List;

import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;

/**
 * {@link FrameProcessor} which detects faces on camera frames.
 * <p>
 * Use {@link #getBuilder()} to create a new instance.
 */
public class PreviewDetectionProcessor implements FrameProcessor {

    private static String TAG = "PreviewDetectionProcessor";

    private static Handler MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    private static final float MINIMUM_CONFIDENCE = 0.3f;

    private final ObjectDetector objectDetector;
    private final OnObjectsDetectedListener listener;

    private PreviewDetectionProcessor(Builder builder){
        objectDetector = builder.objectDetector;
        listener = builder.listener;
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    @Override
    public void processFrame(Frame frame) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int width = frame.size.width;
        int height = frame.size.height;
        YuvImage yuvImage = new YuvImage(frame.image, ImageFormat.NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);


        byte[] imageBytes = out.toByteArray();
        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        List<Recognition> results = Utils.getRecognitionResult(
                objectDetector, image, frame.rotation, MINIMUM_CONFIDENCE);

        MAIN_THREAD_HANDLER.post(() -> listener.onObjectsDetected(results));
    }

    /**
     * Notified when faces are detected.
     */
    public interface OnObjectsDetectedListener {

        /**
         * Null-object for {@link OnObjectsDetectedListener}.
         */
        OnObjectsDetectedListener NULL = faces -> {
            // Do nothing
        };

        /**
         * Called when faces are detected. Always called on the main thread.
         *
         * @param faces detected faces. If no faces were detected - an empty list.
         */
        void onObjectsDetected(List<Recognition> faces);

    }

    /**
     * Builder for {@link PreviewDetectionProcessor}.
     */
    public static class Builder {

        private OnObjectsDetectedListener listener = OnObjectsDetectedListener.NULL;
        private ObjectDetector objectDetector = null;

        private Builder() {
        }

        /**
         * @param objectDetector the object detector to detect objects.
         */
        public Builder detector(ObjectDetector objectDetector) {
            this.objectDetector = objectDetector;
            return this;
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

        public PreviewDetectionProcessor build(){
            return new PreviewDetectionProcessor(this);
        }

    }

}
