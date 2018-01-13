package com.zouyao.objectdetector;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * github.com/tensorflow/models/tree/master/research/object_detection
 */
public class ObjectDetector {
    private static final String TAG = "ObjectDetector";
    // Only return this many results.
    private static final int MAX_RESULTS = 100;
    public static final int INPUT_SIZE = 300;
    private static final String MODEL_FILE =
            "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    private static final String LABELS_FILE = "file:///android_asset/coco_labels_list.txt";
    private static ObjectDetector sObjectDetector = null;
    public static ObjectDetector get(final Context context){
        if (sObjectDetector == null) {
            sObjectDetector = new ObjectDetector(
                    context, MODEL_FILE, LABELS_FILE, INPUT_SIZE);
        }
        return sObjectDetector;
    }


    // Config values.
    private String inputName;
    private int inputSize;
    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<>();
    private String[] outputNames;

    private TensorFlowInferenceInterface inferenceInterface;

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param context  The context, used for its asset manager and toast showing.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     */
    private ObjectDetector(final Context context, final String modelFilename,
                           final String labelFilename, final int inputSize) {
        AssetManager assetManager = context.getAssets();
        InputStream labelsInput;
        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        try {
            labelsInput = assetManager.open(actualFilename);
            BufferedReader br;
            br = new BufferedReader(new InputStreamReader(labelsInput));
            String line;
            while ((line = br.readLine()) != null) {
                Log.w(TAG, line);
                this.labels.add(line);
            }
            br.close();
        }catch (IOException e){
            Toast toast = Toast.makeText(
                    context, "cannot read assets, reinstall the app", Toast.LENGTH_SHORT);
            toast.show();
        }

        this.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);

        final Graph g = this.inferenceInterface.graph();

        this.inputName = "image_tensor";
        // The inputName node has a shape of [N, H, W, C], where
        // N is the batch size
        // H = W are the height and width
        // C is the number of channels (3 for our purposes - RGB)
        final Operation inputOp = g.operation(this.inputName);
        if (inputOp == null) {
            throw new RuntimeException("Failed to find input Node '" + this.inputName + "'");
        }
        this.inputSize = inputSize;
        // The outputScoresName node has a shape of [N, NumLocations], where N
        // is the batch size.
        final Operation outputOp1 = g.operation("detection_scores");
        if (outputOp1 == null) {
            throw new RuntimeException("Failed to find output Node 'detection_scores'");
        }
        final Operation outputOp2 = g.operation("detection_boxes");
        if (outputOp2 == null) {
            throw new RuntimeException("Failed to find output Node 'detection_boxes'");
        }
        final Operation outputOp3 = g.operation("detection_classes");
        if (outputOp3 == null) {
            throw new RuntimeException("Failed to find output Node 'detection_classes'");
        }

        // Pre-allocate buffers.
        this.outputNames = new String[]{"detection_boxes", "detection_scores",
                "detection_classes", "num_detections"};

    }

    public synchronized List<Recognition> recognizeImage(final Bitmap bitmap) {

        int []intValues = new int[this.inputSize * this.inputSize];
        byte []byteValues = new byte[this.inputSize * this.inputSize * 3];
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            byteValues[i * 3 + 2] = (byte) (intValues[i] & 0xFF);
            byteValues[i * 3 + 1] = (byte) ((intValues[i] >> 8) & 0xFF);
            byteValues[i * 3 + 0] = (byte) ((intValues[i] >> 16) & 0xFF);
        }

        // Copy the input data into TensorFlow.

        inferenceInterface.feed(inputName, byteValues, 1, inputSize, inputSize, 3);

        // Run the inference call.
        inferenceInterface.run(outputNames, false);

        // Copy the output Tensor back into the output array.
        float []outputLocations = new float[MAX_RESULTS * 4];
        float []outputScores = new float[MAX_RESULTS];
        float []outputClasses = new float[MAX_RESULTS];
        float []outputNumDetections = new float[1];
        inferenceInterface.fetch(outputNames[0], outputLocations);
        inferenceInterface.fetch(outputNames[1], outputScores);
        inferenceInterface.fetch(outputNames[2], outputClasses);
        inferenceInterface.fetch(outputNames[3], outputNumDetections);

        // Find the best detections.
        final PriorityQueue<Recognition> pq =
                new PriorityQueue<>(
                        1,
                        (lhs, rhs) -> {
                            // Intentionally reversed to put high confidence at the head of the queue.
                            return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                        });

        // Scale them back to the input size.
        for (int i = 0; i < outputScores.length; ++i) {
            final RectF detection =
                    new RectF(
                            outputLocations[4 * i + 1] * inputSize,
                            outputLocations[4 * i] * inputSize,
                            outputLocations[4 * i + 3] * inputSize,
                            outputLocations[4 * i + 2] * inputSize);
            pq.add(
                    new Recognition("" + i, labels.get((int) outputClasses[i]), outputScores[i], detection));
        }

        final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
        for (int i = 0; i < Math.min(pq.size(), MAX_RESULTS); ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }

    public void close() {
        inferenceInterface.close();
    }
}
