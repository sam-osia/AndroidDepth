package com.example.androiddepth;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.CAMERA_SERVICE;

public class Camera extends CameraDevice.StateCallback {

    private static final String TAG = Camera.class.getSimpleName();

    private static int FPS_MIN = 15;
    private static int FPS_MAX = 30;

    private Context context;
    private CameraManager cameraManager;
    private ImageReader depthReader;
    private ImageReader rgbReader;
    private CaptureRequest.Builder previewBuilder;
    private ImageReader.OnImageAvailableListener depthAvailableListener;

    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;

    public Camera(Context context, IDepthFrameListener IDepthFrameListener)
    {
        this.context = context;
        cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        depthAvailableListener = new DepthFrameListener(IDepthFrameListener);
        depthReader = ImageReader.newInstance(
                DepthFrameListener.WIDTH,
                DepthFrameListener.HEIGHT,
                ImageFormat.DEPTH16, 30);
        depthReader.setOnImageAvailableListener(depthAvailableListener, null);

        String cameraId = this.GetCameraId();
        this.OpenCamera(cameraId);
    }

    private String GetCameraId()
    {
        String cameraString = null;
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                boolean rearLens = cameraCharacteristics.get
                        (CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK;
                Boolean supportDepth = cameraCharacteristics.get
                        (CameraCharacteristics.DEPTH_DEPTH_IS_EXCLUSIVE);
                SizeF sensorSize = cameraCharacteristics.get
                        (CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                Size sensorRes = cameraCharacteristics.get
                        (CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);

                int[] capabilities = cameraCharacteristics.get
                        (CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

                String capabilityString = "";

                for (int capability : capabilities) {
                    if (capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) {
                        cameraString = cameraId;
                    }

                    capabilityString = capabilityString + " " + capability;
                }

                String msg = MessageFormat.format
                        ("Camera Id: {0}\nRear lens: {1}\nDepth + RGB: {2}\nSensor size: " +
                                        "{3}\nSensor resolution: {4}\nCapabilities: {5}\n\n",
                                cameraId,
                                rearLens,
                                supportDepth,
                                sensorSize,
                                sensorRes,
                                capabilityString);

                Log.i("CameraInfo", msg);
            }
        }
        catch (CameraAccessException e) { }
        return cameraString;
    }

    private void OpenCamera(String cameraId) {

        Log.i(TAG, "Selected camera: " + cameraId);
        Toast.makeText(context, "connection made", Toast.LENGTH_SHORT).show();
        try
        {
            int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA);

            if (permission == PackageManager.PERMISSION_GRANTED)
            {
                cameraManager.openCamera(cameraId, this, backgroundHandler);
            }
            else
            {
                Log.e(TAG,"Permission not available to open camera");
            }
        }
        catch (CameraAccessException | IllegalStateException | SecurityException e)
        {
            Log.e(TAG,"Opening Camera has an Exception " + e);
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onOpened(@NonNull CameraDevice camera) {
        Log.i(TAG, "opened camera: " + camera.getId());

        try
        {
            previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
            Range<Integer> fpsRange = new Range<>(FPS_MIN, FPS_MAX);
            previewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
            previewBuilder.addTarget(depthReader.getSurface());

            List<Surface> targetSurfaces = Arrays.asList(depthReader.getSurface());

            camera.createCaptureSession(targetSurfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            Log.i(TAG, "Capture session configured");
                            previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                            try {
                                session.setRepeatingRequest(previewBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Capture session failed!");
                        }
                    }, null);

        }

        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice camera) {
        Log.i(TAG, "disconnected camera: " + camera.getId());
        camera.close();
    }

    @Override
    public void onError(@NonNull CameraDevice camera, int error) {
        Log.i(TAG, "error from camera: " + camera.getId());
        camera.close();
    }

    private void StartBackgroundThread()
    {
        backgroundHandlerThread = new HandlerThread("CameraThread");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }

    private void StopBackgroundThread()
    {
        backgroundHandlerThread.quitSafely();
        try {
            backgroundHandlerThread.join();
            backgroundHandlerThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
