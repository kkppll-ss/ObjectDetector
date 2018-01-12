package com.zouyao.objectdetector;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

import io.fotoapparat.Fotoapparat;

import com.zouyao.objectdetector.view.RecognitionView;

import io.fotoapparat.parameter.LensPosition;
import io.fotoapparat.parameter.ScaleType;
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
    private ObjectDetectorProcessor objectDetectorProcessor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = (CameraView) findViewById(R.id.camera_view);
        recognitionView = (RecognitionView) findViewById(R.id.recognitionView);
        hasCameraPermission = permissionsDelegate.hasCameraPermission();

        if (hasCameraPermission) {
            cameraView.setVisibility(View.VISIBLE);
        } else {
            permissionsDelegate.requestCameraPermission();
        }

        try {
            objectDetectorProcessor = ObjectDetectorProcessor.with(this)
                    .listener(new ObjectDetectorProcessor.OnObjectsDetectedListener() {
                        @Override
                        public void onObjectsDetected(List<Recognition> faces) {
                            recognitionView.setRecognitions(faces);
                        }
                    })
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }

        camera = createFotoapparat();

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
                        objectDetectorProcessor
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
