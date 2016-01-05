package com.app.mycamera;

import android.app.Activity;
import android.graphics.Point;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import com.app.camerainterface.CameraInterface;
import com.app.camerapreview.CameraSurfaceView;
import com.app.util.DisplayUtil;
import com.app.util.MyLog;


public class CameraActivity extends Activity implements CameraInterface.CamOpenOverCallback{

    private CameraSurfaceView surfaceView=null;//自定义的SurfaceView
    private ImageButton shutterBtn=null;
    private float previewRate=-1f;
    private final static String TAG="admin";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//去掉信息栏
        setContentView(R.layout.activity_camera);
        //初始化界面
        initViewParams();

        shutterBtn.setOnClickListener(new ButtonListener());
    }

    @Override
    protected void onStop() {
        super.onStop();
        CameraInterface.getInstance().stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                CameraInterface.getInstance().openCamera(CameraActivity.this);
                MyLog.i(TAG,"线程结束了");
            }
        }).start();
    }

    /*    private void initUI(){
            surfaceView = (CameraSurfaceView)findViewById(R.id.camera_surfaceview);
            shutterBtn = (ImageButton)findViewById(R.id.btn_shutter);
        }*/
    private void initViewParams(){
        surfaceView = (CameraSurfaceView)findViewById(R.id.camera_surfaceview);
        shutterBtn = (ImageButton)findViewById(R.id.btn_shutter);

        ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
        Point p = DisplayUtil.getScreenMetrics(this);
        params.width = p.x;//单位为像素？
        params.height = p.y;
        previewRate = DisplayUtil.getScreenRate(this); //默认全屏的比例预览
        surfaceView.setLayoutParams(params);

        //手动设置拍照ImageButton的大小为90dip×90dip,原图片大小是64×64
        ViewGroup.LayoutParams p2 = shutterBtn.getLayoutParams();
        p2.width = DisplayUtil.dip2px(this, 90);
        p2.height = DisplayUtil.dip2px(this, 90);
        shutterBtn.setLayoutParams(p2);

    }

    /**
     * 打开摄像头时的回调方法，开启预览
     */
    @Override
    public void cameraHasOpened() {
        SurfaceHolder holder=surfaceView.getSurfaceHolder();
        CameraInterface.getInstance().startPreview(holder,previewRate);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch(v.getId()){
                case R.id.btn_shutter:
                    CameraInterface.getInstance().takePicture();
                    break;
                default:break;
            }
        }

    }
}
