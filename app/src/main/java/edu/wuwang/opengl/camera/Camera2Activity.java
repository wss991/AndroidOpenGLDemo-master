/*
 *
 * Camera2Activity.java
 *
 * Created by Wuwang on 2017/3/6
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package edu.wuwang.opengl.camera;

import static android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import edu.wuwang.opengl.BaseActivity;
import edu.wuwang.opengl.R;
import edu.wuwang.opengl.encoder.AudioRecorderHandlerThread;
import edu.wuwang.opengl.encoder.Messages;
import edu.wuwang.opengl.encoder.VideoEncoder;
import edu.wuwang.opengl.filter.TimeMarkFilter;
import edu.wuwang.opengl.filter.ZipPkmAnimationFilter;
import edu.wuwang.opengl.utils.CompareSizesByArea;
import edu.wuwang.opengl.utils.PermissionUtils;


/**
 * Description:
 */
public class Camera2Activity extends BaseActivity implements FrameCallback {

    private SurfaceView mSurfaceView;
    private TextureController mController;
    private Renderer mRenderer;
    private int cameraId = 0;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording; // 是否正在录制
    private SurfaceHolder mHolder;
    private int outWidth;
    private int outHeight;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        PermissionUtils.askPermission(this, new String[]{Manifest.permission.CAMERA, Manifest
                .permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 10, initViewRunnable);

    }

    protected void onFilterSet(TextureController controller) {
        ZipPkmAnimationFilter mAniFilter = new ZipPkmAnimationFilter(getResources());
        mAniFilter.setAnimation("assets/etczip/cc.zip");
        controller.addFilter(mAniFilter);
    }

    protected void setContentView() {
        setContentView(R.layout.activity_camera2);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Messages.MSG_CHANGE_BUTTON_ENABLED:

                    break;
                case Messages.MSG_RECORDING_START_CALLBACK:

                    break;
                case Messages.MSG_RECORDING_STOP_CALLBACK:

                    break;
            }
        }
    };
    private Runnable initViewRunnable = new Runnable() {
        @Override
        public void run() {

            //TODO 设置数据源
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRenderer = new Camera2Renderer();
            } else {
                mRenderer = new Camera1Renderer();
            }
            setContentView();
            mSurfaceView = (SurfaceView) findViewById(R.id.mSurface);
            mController = new TextureController(Camera2Activity.this);
            TimeMarkFilter filter = new TimeMarkFilter(getResources());
            filter.setWaterMark(BitmapFactory.decodeResource(getResources(), R.mipmap.logo));
            filter.setPosition(0, 0, 300, 150);
            mController.addFilter(filter);
            //  onFilterSet(mController);

            mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mHolder = holder;
                    mController.surfaceCreated(holder);
                    mController.setRenderer(mRenderer);
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    Log.e("Camera2Renderer", "surfaceChanged: " + width + " " + height);
                    mController.surfaceChanged(width, height);
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mController.surfaceDestroyed();
                }
            });


        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.onRequestPermissionsResult(requestCode == 10, grantResults, initViewRunnable,
                new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Camera2Activity.this, "没有获得必要的权限", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.mShutter:
                mController.takePhoto();
                if (isRecording) {
                    mController.stopRecord();
                } else {
                    mController.startRecord(getExternalCacheDir() + File.separator + "mp4record.mp4");
                }
                isRecording = !isRecording;
               // startRecording();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mController != null) {
            mController.onResume();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mController != null) {
            mController.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        stopRecording();
        super.onDestroy();
        if (mController != null) {
            mController.destroy();
        }


    }

    @Override
    public void onFrame(final byte[] bytes, long time) {
/*        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = Bitmap.createBitmap(mController.getmDataSize().x, mController.getmDataSize().y, Bitmap.Config.ARGB_8888);
                ByteBuffer b = ByteBuffer.wrap(bytes);
                bitmap.copyPixelsFromBuffer(b);
                saveBitmap(bitmap);
                bitmap.recycle();
            }
        }).start();*/
    }

    protected String getSD() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    //图片保存
    public void saveBitmap(Bitmap b) {
        String path = getSD() + "/OpenGLDemo/photo/";
        File folder = new File(path);
        if (!folder.exists() && !folder.mkdirs()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Camera2Activity.this, "无法保存照片", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        long dataTake = System.currentTimeMillis();
        final String jpegName = path + dataTake + ".jpg";
        try {
            FileOutputStream fout = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Camera2Activity.this, "保存成功->" + jpegName, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private Camera camera;

    private class Camera1Renderer implements Renderer {

        private Camera mCamera;

        @Override
        public void onDestroy() {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            mCamera = Camera.open(cameraId);
            camera = mCamera;
            mController.setImageDirection(cameraId);
            Camera.Size size = mCamera.getParameters().getPreviewSize();
            mController.setDataSize(size.height, size.width);
            mController.setFrameCallback(720, 1280, Camera2Activity.this);
            try {
                mCamera.setPreviewTexture(mController.getTexture());
                mController.getTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        mController.requestRender();
                    }
                });


            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {

        }

    }

    private void startRecording() {
        try {
            camera.unlock();
            mMediaRecorder = new MediaRecorder();
           /* String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/ych/123.mp4";
            new File(filePath);*/
            File file = new File(getExternalCacheDir() + "/1234.mp4");
            mMediaRecorder.reset();
            mMediaRecorder.setCamera(camera);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
//            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
//            mMediaRecorder.setVideoSize(320, 240);//每个手机的屏幕视频都不一样，需要调整
//            mMediaRecorder.setVideoFrameRate(4);
            mMediaRecorder.setVideoEncoder(MediaRecorder
                    .VideoEncoder.H264);
            mMediaRecorder.setVideoSize(1280, 720);
            // 每秒 4帧
            mMediaRecorder.setOrientationHint(90);
            mMediaRecorder.setVideoFrameRate(20);
            mMediaRecorder.setPreviewDisplay(mHolder.getSurface());  // ①
            mMediaRecorder.setOutputFile(file.getAbsolutePath());
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            isRecording = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "成功", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        if (mMediaRecorder != null) {
            isRecording = false;
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mController!=null) mController.stopRecord();


    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class Camera2Renderer implements Renderer {

        CameraDevice mDevice;
        CameraManager mCameraManager;
        private HandlerThread mThread;
        private Handler mHandler;
        private Size mPreviewSize;

        private int realPreviewWidth = 0;
        private int realPreviewHeight = 0;
        private static final String TAG = "Camera2Renderer";

        Camera2Renderer() {
            mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            mThread = new HandlerThread("camera2 ");
            mThread.start();
            mHandler = new Handler(mThread.getLooper());
        }

        @Override
        public void onDestroy() {
            if (mDevice != null) {
                mDevice.close();
                mDevice = null;
            }
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            extracted();
        }

        private void extracted() {
            try {
                if (mDevice != null) {
                    mDevice.close();
                    mDevice = null;
                }
                CameraCharacteristics c = mCameraManager.getCameraCharacteristics(cameraId + "");
                StreamConfigurationMap map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);


                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                Point displaySize = new Point();
                Camera2Activity.this.getWindowManager().getDefaultDisplay().getSize(displaySize);
                realPreviewWidth = mSurfaceView.getWidth();
                realPreviewHeight = mSurfaceView.getHeight();
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                //自定义规则，选个大小

                int displayRotation = Camera2Activity.this.getWindowManager().getDefaultDisplay().getRotation();
                if (displayRotation == 90 || displayRotation == 270) {
                    realPreviewWidth = mSurfaceView.getHeight();
                    realPreviewHeight = mSurfaceView.getWidth();

                }

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        realPreviewWidth, realPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                Log.e(TAG, "extracted: " + mSurfaceView.getWidth() + "  " + mSurfaceView.getHeight());
                Log.e(TAG, "extracted: " + realPreviewWidth + "  " + realPreviewHeight);
                Log.e(TAG, "extracted: mPreviewSize " + mPreviewSize.getWidth() + "  " + mPreviewSize.getHeight());


                if (displayRotation == 90 || displayRotation == 270) {
                    //     mController.setScale(mPreviewSize.getHeight()*100/(realPreviewWidth/100f), mPreviewSize.getWidth()*100/(realPreviewHeight/100f));
                    realPreviewWidth = mPreviewSize.getHeight();
                    realPreviewHeight = mPreviewSize.getWidth();

                } else {
                    //  mController.setScale(mPreviewSize.getWidth()*100/realPreviewWidth/100f, mPreviewSize.getHeight()*100/realPreviewHeight/100f);

                    realPreviewWidth = mPreviewSize.getWidth();
                    realPreviewHeight = mPreviewSize.getHeight();
                }
                mController.setDataSize(realPreviewWidth, realPreviewHeight);
                mController.setFrameCallback(realPreviewWidth, realPreviewHeight, Camera2Activity.this);
                mCameraManager.openCamera(cameraId + "", new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        mDevice = camera;
                        try {
                            Surface surface = new Surface(mController
                                    .getTexture());
                            final CaptureRequest.Builder builder = mDevice.createCaptureRequest
                                    (TEMPLATE_PREVIEW);
                            builder.addTarget(surface);
                            mController.getTexture().setDefaultBufferSize(
                                    realPreviewWidth, realPreviewHeight);
                            mDevice.createCaptureSession(Arrays.asList(surface), new
                                    CameraCaptureSession.StateCallback() {
                                        @Override
                                        public void onConfigured(CameraCaptureSession session) {
                                            try {
                                                session.setRepeatingRequest(builder.build(), new CameraCaptureSession.CaptureCallback() {
                                                    @Override
                                                    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
                                                        super.onCaptureProgressed(session, request, partialResult);
                                                    }

                                                    @Override
                                                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                                        super.onCaptureCompleted(session, request, result);
                                                        mController.requestRender();
                                                    }
                                                }, mHandler);
                                            } catch (CameraAccessException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onConfigureFailed(CameraCaptureSession session) {

                                        }
                                    }, mHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onDisconnected(CameraDevice camera) {
                        mDevice = null;
                    }

                    @Override
                    public void onError(CameraDevice camera, int error) {

                    }
                }, mHandler);


            } catch (SecurityException | CameraAccessException e) {
                e.printStackTrace();
            }
        }

        private Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                       int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

            // Collect the supported resolutions that are at least as big as the preview Surface
            List<Size> bigEnough = new ArrayList<>();
            // Collect the supported resolutions that are smaller than the preview Surface
            List<Size> notBigEnough = new ArrayList<>();
            int w = aspectRatio.getWidth();
            int h = aspectRatio.getHeight();
            for (Size option : choices) {
                if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                        option.getHeight() == option.getWidth() * h / w) {
                    if (option.getWidth() >= textureViewWidth &&
                            option.getHeight() >= textureViewHeight) {
                        bigEnough.add(option);
                    } else {
                        notBigEnough.add(option);
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size() > 0) {
                return Collections.min(bigEnough, new CompareSizesByArea());
            } else if (notBigEnough.size() > 0) {
                return Collections.max(notBigEnough, new CompareSizesByArea());
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size");
                return choices[0];
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {

        }

    }


}
