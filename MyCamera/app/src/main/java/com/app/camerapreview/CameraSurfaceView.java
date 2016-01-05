package com.app.camerapreview;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.app.camerainterface.CameraInterface;
import com.app.util.MyLog;

/**
 * Created by admin on 2016/1/5.
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
    private final static String TAG="admin";
    //CameraInterface mCameraInterface;
    Context mContext;
    SurfaceHolder mSurfaceHolder;
    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);//translucent半透明
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);
    }
    public SurfaceHolder getSurfaceHolder(){
        return mSurfaceHolder;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        MyLog.i(TAG, "surfaceDestroyed...");
        //SurfaceView被遮住后，生命周期结束
        CameraInterface.getInstance().stopCamera();
    }
}
