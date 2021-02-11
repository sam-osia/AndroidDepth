package com.example.androiddepth;

import android.graphics.Bitmap;
import android.media.Image;

public interface DepthFrameVisualizer {
    boolean getRenderDepth();
    boolean getRenderConfidence();
    boolean getProcessLive();
    void onRawDataAvailable(Image image);
    void onDepthMapAvailable(Bitmap bitmap);
    void onConfidenceAvailable(Bitmap bitmap);
    void onCenterDepthAvailable(int distance);
    void onFpsAvailable(double fps);
}
