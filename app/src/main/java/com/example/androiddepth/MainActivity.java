package com.example.androiddepth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ShortBuffer;
import java.text.MessageFormat;

public class MainActivity extends AppCompatActivity implements DepthFrameVisualizer {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int CAM_PERMISSIONS_REQUEST = 0;
    private TextureView tvDepth;
    private TextureView tvConfidence;
    private Button btnRequestCapture;
    private boolean _capture;

    private Switch swDepth;
    private Switch swConfidence;
    private Switch swProcessLive;
    private TextView txtFps;

    public boolean renderDepth;
    public boolean renderConfidence;
    public boolean processLive;

    private Camera camera;

    long prevTime = System.currentTimeMillis();
    String fileName;

    private Matrix defaultBitmapTransform;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDepth = findViewById(R.id.tvDepth);
        tvConfidence = findViewById(R.id.tvConfidence);
        btnRequestCapture = findViewById(R.id.btnRequestCapture);

        swDepth = findViewById(R.id.swDepth);
        swConfidence = findViewById(R.id.swConfidence);
        swProcessLive = findViewById(R.id.swProcessLive);
        txtFps = findViewById(R.id.txtFps);

        renderDepth = swDepth.isChecked();
        renderConfidence = swConfidence.isChecked();
        processLive = swProcessLive.isChecked();

        swProcessLive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                processLive = isChecked;
                if (processLive)
                {
                    swDepth.setVisibility(View.VISIBLE);
                    swConfidence.setVisibility(View.VISIBLE);
                }
                else
                {
                    swDepth.setVisibility(View.INVISIBLE);
                    swConfidence.setVisibility(View.INVISIBLE);
                }
            }
        });

        swDepth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                renderDepth = isChecked;
            }
        });

        swConfidence.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                renderConfidence = isChecked;
            }
        });

        // writeToSDFile();

        this.checkPermissions();
        camera = new Camera(this, this);
    }

    private void checkPermissions()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAM_PERMISSIONS_REQUEST);
        }
    }

    @Override
    public boolean getRenderDepth() {
        return renderDepth;
    }

    @Override
    public boolean getRenderConfidence() {
        return renderConfidence;
    }

    @Override
    public boolean getProcessLive() {
        return processLive;
    }

    @Override
    public void onDepthMapAvailable(Bitmap bitmap) {
        RenderBitmapToTextureView(bitmap, tvDepth);
    }

    @Override
    public void onConfidenceAvailable(Bitmap bitmap) {
        RenderBitmapToTextureView(bitmap, tvConfidence);
    }

    @Override
    public void onRawDataAvailable(Image image) {

        if (!this._capture)
            return;

        long now = System.currentTimeMillis();
        Log.i(TAG, String.valueOf(now - prevTime));
        prevTime = now;

        File root = this.getExternalFilesDir(null);

        File dir = new File (root.getAbsolutePath() + "/Depth");
        boolean fileMade = dir.mkdirs();

        File file = new File(dir, fileName);

        FileOutputStream f = null;
        try {
            f = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        PrintWriter pw = new PrintWriter(f);

        int WIDTH = DepthFrameAvailableListener.WIDTH;
        int HEIGHT = DepthFrameAvailableListener.HEIGHT;

        ShortBuffer shortDepthBuffer = image.getPlanes()[0].getBuffer().asShortBuffer();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = y * WIDTH + x;
                short depthSample = shortDepthBuffer.get(index);
                pw.print(depthSample);
                pw.print(",");
            }
            pw.println();
        }

        pw.flush();
        pw.close();
        try {
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateFileName()
    {
        File root = this.getExternalFilesDir(null);

        File dir = new File (root.getAbsolutePath() + "/Depth");
        boolean fileMade = dir.mkdirs();

        int numFiles = dir.listFiles().length;
        String newFileName = MessageFormat.format("{0}.txt", numFiles + 1);
        return newFileName;
    }

    @Override
    public void onCenterDepthAvailable(int distance) {
        Log.i(TAG, String.valueOf(distance));
    }

    @Override
    public void onFpsAvailable(double fps) {
        txtFps.setText("FPS: " + (int)fps);
    }

    private void RenderBitmapToTextureView(Bitmap bitmap, TextureView textureView) {
        Canvas canvas = textureView.lockCanvas();
        canvas.drawBitmap(bitmap, defaultBitmapTransform(textureView), null);
        textureView.unlockCanvasAndPost(canvas);
    }

    private Matrix defaultBitmapTransform(TextureView view) {
        if (defaultBitmapTransform == null || view.getWidth() == 0 || view.getHeight() == 0) {
            Matrix matrix = new Matrix();
            int centerX = view.getWidth() / 2;
            int centerY = view.getHeight() / 2;

            int bufferWidth = DepthFrameAvailableListener.WIDTH;
            int bufferHeight = DepthFrameAvailableListener.HEIGHT;

            RectF bufferRect = new RectF(0, 0, bufferWidth, bufferHeight);
            RectF viewRect = new RectF(0, 0, view.getWidth(), view.getHeight());
            matrix.setRectToRect(bufferRect, viewRect, Matrix.ScaleToFit.CENTER);
            matrix.postRotate(90, centerX, centerY);

            defaultBitmapTransform = matrix;
        }
        return defaultBitmapTransform;
    }

    public void btnRequestCapture_onClick(View view) {
        if (this._capture)
        {
            btnRequestCapture.setText("Start Capture");
            fileName = null;
        }

        else
        {
            btnRequestCapture.setText("Stop Capture");
            fileName = generateFileName();
        }

        this._capture = !this._capture;
    }
}