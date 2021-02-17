package com.example.androiddepth;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;

import java.nio.ShortBuffer;

public class DepthFrameListener implements ImageReader.OnImageAvailableListener
{
    private static final String TAG = DepthFrameListener.class.getSimpleName();
    public static final int WIDTH = 240;
    public static final int HEIGHT = 180;
    public static final int FRAME_COUNT_MAX = 10;

    private int numFrames;
    private long prevTime;

    private IDepthFrameListener depthFrameListener;

    public DepthFrameListener(IDepthFrameListener depthFrameListener)
    {
        this.depthFrameListener = depthFrameListener;
        numFrames = 0;
        prevTime = System.currentTimeMillis();
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireNextImage();
        if (image != null && image.getFormat() == ImageFormat.DEPTH16)
        {
            numFrames++;
            depthFrameListener.onRawDepthAvailable(image);

            if (depthFrameListener.getProcessLive()) {
                PublishRaw(image);
            }
        }
        image.close();

        if (numFrames == FRAME_COUNT_MAX)
        {
            numFrames = 0;
            long now = System.currentTimeMillis();
            long duration = now - prevTime;
            try
            {
                depthFrameListener.onFpsAvailable((10.0 / duration) * 1000);
                prevTime = now;
            }catch (ArithmeticException e) { }
        }
    }

    private void PublishRaw(Image image)
    {
        int[] depthData = new int[WIDTH * HEIGHT];
        int[] confidenceData = new int[WIDTH * HEIGHT];
        int centerDistance = -1;

        ShortBuffer shortDepthBuffer = image.getPlanes()[0].getBuffer().asShortBuffer();

        for (int y = 0; y < HEIGHT; y++)
        {
            for (int x = 0; x < WIDTH; x++)
            {
                int index = y * WIDTH + x;
                short depthSample = shortDepthBuffer.get(index);
                short depthRange = (short) (depthSample & 0x1FFF);

                if (y == HEIGHT / 2 && x == WIDTH / 2)
                {
                    centerDistance = depthRange;
                }

                short depthConfidence = (short) ((depthSample >> 13) & 0x7);
                float depthPercentage = depthConfidence == 0 ? 1.f : (depthConfidence - 1) / 7.f;
                int depthPercentageColor = (int)(depthPercentage * 255);

                depthData[index] = depthRange;
                confidenceData[index] = depthPercentageColor;
            }
        }

        if (depthFrameListener != null)
        {
            if (depthFrameListener.getRenderDepth())
            {
                Bitmap depthBitmap = ConvertToRGBBitmap(depthData);
                depthFrameListener.onDepthMapAvailable(depthBitmap);
                depthBitmap.recycle();
            }

            if (depthFrameListener.getRenderConfidence())
            {
                Bitmap confidenceBitmap = ConvertToRGBBitmap(confidenceData);
                depthFrameListener.onConfidenceAvailable(confidenceBitmap);
                confidenceBitmap.recycle();
            }
        }
    }

    private Bitmap ConvertToRGBBitmap(int[] mask) {
        Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_4444);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = y * WIDTH + x;
                int temp = mask[index];
                int color = Color.argb(255, 0, mask[index],0);
                int A = (color >> 24) & 0xff; // or color >>> 24
                int R = (color >> 16) & 0xff;
                int G = (color >>  8) & 0xff;
                int B = (color      ) & 0xff;
                bitmap.setPixel(x, y, Color.argb(255, 0, mask[index],0));
            }
        }
        return bitmap;
    }
}
