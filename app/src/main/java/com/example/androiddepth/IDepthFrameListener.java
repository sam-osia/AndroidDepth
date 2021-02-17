package com.example.androiddepth;

import android.graphics.Bitmap;
import android.media.Image;

public interface IDepthFrameListener {
    boolean getRenderDepth();
    boolean getRenderConfidence();
    boolean getProcessLive();
    void onRawDepthAvailable(Image image);
    void onDepthMapAvailable(Bitmap bitmap);
    void onConfidenceAvailable(Bitmap bitmap);
    void onFpsAvailable(double fps);
}
