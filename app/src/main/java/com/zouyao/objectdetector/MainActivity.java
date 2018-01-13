package com.zouyao.objectdetector;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.fotoapparat.Fotoapparat;

import com.zouyao.objectdetector.view.RecognitionView;

import io.fotoapparat.parameter.LensPosition;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.photo.BitmapPhoto;
import io.fotoapparat.result.PhotoResult;
import io.fotoapparat.view.CameraView;

import static io.fotoapparat.log.Loggers.fileLogger;
import static io.fotoapparat.log.Loggers.logcat;
import static io.fotoapparat.log.Loggers.loggers;
import static io.fotoapparat.parameter.selector.FlashSelectors.autoFlash;
import static io.fotoapparat.parameter.selector.FlashSelectors.autoRedEye;
import static io.fotoapparat.parameter.selector.FlashSelectors.torch;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.autoFocus;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.continuousFocus;
import static io.fotoapparat.parameter.selector.FocusModeSelectors.fixed;
import static io.fotoapparat.parameter.selector.LensPositionSelectors.lensPosition;
import static io.fotoapparat.parameter.selector.Selectors.firstAvailable;
import static io.fotoapparat.parameter.selector.SizeSelectors.biggestSize;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    private final PermissionsDelegate permissionsDelegate = new PermissionsDelegate(this);
    private boolean hasCameraPermission;
    private CameraView cameraView;
    private RecognitionView recognitionView;
    private Fotoapparat camera;
    private FloatingActionButton takePictureBottom;
    private ObjectDetector objectDetector = null;

    private class RecognizeTakenPhotoTask extends AsyncTask<Object,Void,String> {
        @Override
        protected String doInBackground(Object... params) {
            BitmapPhoto bitmapPhoto = (BitmapPhoto) params[0];
            File rcgImageFile = (File)params[1];
            Bitmap bitmap = bitmapPhoto.bitmap;
            int frameRotation = bitmapPhoto.rotationDegrees;
            List<Recognition> recognitions = Utils.getRecognitionResult(
                    objectDetector, bitmap, frameRotation, 0.3f);
            int rotation = Utils.getRotation(frameRotation);
            Matrix matrix = new Matrix();
            matrix.setRotate(rotation);
            Bitmap rotatedBitmap = null;
            if (rotation != 0) {
                 rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            }
            else {
                rotatedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            }
            Canvas canvas = new Canvas(rotatedBitmap);
            Paint rectPaint = new Paint();
            Paint textPaint = new Paint();
            float textSize = Utils.setAttributes(
                    MainActivity.this, rectPaint, textPaint);
            Utils.drawRecognitions(canvas, recognitions, rectPaint, textPaint, textSize);
            FileOutputStream out = null;
            String returnStr = null;
            try {
                out = new FileOutputStream(rcgImageFile);
                rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                returnStr =  "recognized file saved to " + rcgImageFile.getAbsolutePath();
            } catch (FileNotFoundException e) {
                returnStr = "file not found: " + rcgImageFile.getAbsolutePath();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return returnStr;
        }

        @Override
        protected void onPostExecute(String resultStr){
            Toast toast = Toast.makeText(MainActivity.this, resultStr, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera_view);
        recognitionView = findViewById(R.id.recognition_view);
        takePictureBottom = findViewById(R.id.take_picture_button);
        hasCameraPermission = permissionsDelegate.hasCameraPermission();

        if (hasCameraPermission) {
            cameraView.setVisibility(View.VISIBLE);
        } else {
            permissionsDelegate.requestCameraPermission();
        }
        objectDetector = ObjectDetector.get(this);

        camera = createFotoapparat();
        takePictureBottom.setOnClickListener((view) -> {
            PhotoResult photoResult = camera.autoFocus().takePicture();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            File imagesFolder = getExternalFilesDir(Environment.DIRECTORY_DCIM);
            Log.i(TAG, "image folder is " + Environment.DIRECTORY_DCIM);


            File rawImageFile = new File(imagesFolder, "RAW_" + timeStamp + ".png");
            File rcgImageFile = new File(imagesFolder, "RCG_" + timeStamp + ".png");
            photoResult.saveToFile(rawImageFile).whenDone((result) -> {
                Toast toast = Toast.makeText(MainActivity.this,
                        "raw image saved to file " + rawImageFile.getAbsolutePath(), Toast.LENGTH_SHORT);
                toast.show();
            });
            photoResult
                    .toBitmap()
                    .whenAvailable((bitmapPhoto ->
                            new RecognizeTakenPhotoTask().execute(bitmapPhoto, rcgImageFile)));
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    private Fotoapparat createFotoapparat() {
        Fotoapparat camera;
        camera = Fotoapparat
                .with(this)
                .into(cameraView)
                .lensPosition(lensPosition(LensPosition.BACK))
                .previewScaleType(ScaleType.CENTER_CROP)  // we want the preview to fill the view
                .photoSize(biggestSize())   // we want to have the biggest photo possible
                .focusMode(firstAvailable(  // (optional) use the first focus mode which is supported by device
                        continuousFocus(),
                        autoFocus(),        // in case if continuous focus is not available on device, auto focus will be used
                        fixed()             // if even auto focus is not available - fixed focus mode will be used
                ))
                .flash(firstAvailable(      // (optional) similar to how it is done for focus mode, this time for flash
                        autoRedEye(),
                        autoFlash(),
                        torch()
                ))
                .frameProcessor(
                           PreviewDetectionProcessor
                                .getBuilder()
                                .detector(objectDetector)
                                .listener(recognitions
                                        -> recognitionView.setRecognitions(recognitions))
                                .build()
                )
                .logger(loggers(
                        logcat(),
                        fileLogger(this)
                ))
                .build();
        return camera;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasCameraPermission) {
            camera.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (hasCameraPermission) {
            camera.stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionsDelegate.resultGranted(requestCode, permissions, grantResults)) {
            camera.start();
            cameraView.setVisibility(View.VISIBLE);
        }
    }

}
