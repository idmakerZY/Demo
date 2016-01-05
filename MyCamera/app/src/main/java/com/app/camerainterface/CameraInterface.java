package com.app.camerainterface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;

import com.app.util.*;

import java.io.IOException;
import java.util.List;

/**
 * Created by admin on 2016/1/5.
 */

public class CameraInterface {

    /**
     * 内部实现的回调接口，主Activity实现这个接口，在启动摄像头时回调主Activity的cameraHasOpened()方法，
     * 得到主Activity的SurfaceView的surfaceHolder
     */
    public interface CamOpenOverCallback{
        public void cameraHasOpened();
    }

    private static final String TAG = "admin";
    private Camera mCamera;
    private Camera.Parameters mParams;
    private boolean isPreviewing = false;
    private float mPreviwRate = -1f;
    private int mWidth=800;
    //单例模式
    private static CameraInterface mCameraInterface;
    private CameraInterface() {
    }
    public synchronized static CameraInterface getInstance(){
        if(mCameraInterface==null){
            mCameraInterface=new CameraInterface();
            return mCameraInterface;
        }
        else
        {
            return mCameraInterface;
        }
    }

    /**
     * 打开摄像头
     * @param callback
     */
    public void openCamera(CamOpenOverCallback callback){
        MyLog.i(TAG,"准备打开摄像头...");
        mCamera=Camera.open();
        MyLog.i(TAG,"摄像头已经打开");
        //回调主Activity的这个回调函数，得到界面的SurfaceHolder，开始浏览摄像头得到的画面
        callback.cameraHasOpened();
    }

    /**
     *开始浏览
     * @param holder
     * @param previewRate
     */
    public void startPreview(SurfaceHolder holder, float previewRate){
        MyLog.i(TAG,"开始浏览");
        if(isPreviewing){
            mCamera.stopPreview();
            return;
        }
        if(mCamera!=null)
        {
            mParams=mCamera.getParameters();
            //设置图片存储格式
            mParams.setPictureFormat(PixelFormat.JPEG);
            //打印支持的图片大小和预览大小
            CamParaUtil.getInstance().printSupportPictureSize(mParams);
            CamParaUtil.getInstance().printSupportPreviewSize(mParams);
            //设置图片大小和预览大小
            Size pictureSize=CamParaUtil.getInstance().getPropPictureSize(mParams.getSupportedPictureSizes()
                    ,previewRate,mWidth);
            mParams.setPictureSize(pictureSize.width,pictureSize.height);

            Size previewSize=CamParaUtil.getInstance().getPropPreviewSize(mParams.getSupportedPreviewSizes()
                    ,previewRate,mWidth);
            mParams.setPreviewSize(previewSize.width,previewSize.height);
            MyLog.i(TAG,"previewRate"+previewRate);

            mCamera.setDisplayOrientation(90);

            CamParaUtil.getInstance().printSupportFocusMode(mParams);
            List<String> focusModes = mParams.getSupportedFocusModes();
            if(focusModes.contains("continuous-video")){
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            mCamera.setParameters(mParams);

            //开始预览
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }

            isPreviewing = true;
            mPreviwRate = previewRate;
            MyLog.i(TAG,"previewRate"+mPreviwRate);

            mParams = mCamera.getParameters(); //重新get一次
            MyLog.i(TAG, "最终设置:PreviewSize--Width = " + mParams.getPreviewSize().width
                    + "Height = " + mParams.getPreviewSize().height);
            MyLog.i(TAG, "最终设置:PictureSize--Width = " + mParams.getPictureSize().width
                    + "Height = " + mParams.getPictureSize().height);
        }// end if


    }//end startPreview()

    /**
     * 停止摄像头
     */
    public void stopCamera(){
        if(mCamera!=null)
        {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            isPreviewing=false;
            mPreviwRate=-1f;
            mCamera.release();
            mCamera=null;
            mParams=null;
        }
    }

    /**
     * 拍照
     */
    public void takePicture(){
        if(isPreviewing && (mCamera != null)){
            mCamera.takePicture(mShutterCallback, null, pictureCallbackJpeg);
        }
    }

    //为了实现拍照的快门声音及拍照保存照片需要下面三个回调变量
    Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback()
            //快门按下的回调，在这里我们可以设置类似播放“咔嚓”声之类的操作。默认的就是咔嚓。
    {
        public void onShutter() {
            // TODO Auto-generated method stub
            MyLog.i(TAG, "myShutterCallback:onShutter...");
        }
    };
    Camera.PictureCallback mRawCallback = new Camera.PictureCallback()
            // 拍摄的未压缩原数据的回调,可以为null
    {

        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            MyLog.i(TAG, "myRawCallback:onPictureTaken...");

        }
    };

    Camera.PictureCallback pictureCallbackJpeg=new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap b = null;
            if(null != data){
                b = BitmapFactory.decodeByteArray(data, 0, data.length);//data是字节数据，将其解析成位图
                mCamera.stopPreview();
                isPreviewing = false;
            }

            //保存图片到sdcard
            if(null != b)
            {
                Bitmap rotaBitmap = ImageUtil.getRotateBitmap(b, 90.0f);
                FileUtil.saveBitmap(rotaBitmap);
            }

            //再次进入预览
            mCamera.startPreview();
            isPreviewing = true;

        }
    };
}
