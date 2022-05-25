package edu.wuwang.opengl.encoder;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

/**
 * @author: sq
 * @date: 2017/7/26
 * @corporation: 深圳市思迪信息科技有限公司
 * @description: 音、视频编解码线程
 */
public class AudioRecorderHandlerThread extends HandlerThread implements Handler.Callback {
    //    private static final String TAG = AudioRecorderHandlerThread.class.getSimpleName();
    private static final String TAG = "AudioRecorderThread";

    /* Handler associated with this HandlerThread*/
    private Handler mHandler;

    /* Reference to a handler from the thread that started this HandlerThread */
    private Handler mCallback;

    private static final int MSG_RECORDING_START = 300;
    private static final int MSG_RECORDING_STOP = 101;

    /* AudioRecord object to record audio from microphone input */
    private AudioRecorder audioRecorder;

    /* AudioEncoder object to take recorded ByteBuffer from the AudioRecord object*/
    private AudioEncoder audioEncoder;

    /* VideoEncoder object to take recorded ByteBuffer from the Camera object*/
    public VideoEncoder videoEncoder;

    /* MediaMuxerWrapper object to add encoded data to a MediaMuxer which converts it to .mp4*/
    private MediaMuxerWrapper mediaMuxerWrapper;
    public boolean isStart;


    public AudioRecorderHandlerThread(String name, String outputFile) {
        super(name);
        mediaMuxerWrapper = new MediaMuxerWrapper(outputFile);
        audioEncoder = new AudioEncoder(mediaMuxerWrapper);
        audioRecorder = new AudioRecorder(audioEncoder);
        videoEncoder = new VideoEncoder(mediaMuxerWrapper, 480, 640);
    }

    public AudioRecorderHandlerThread(String name, int priority, String outputFile) {
        super(name, priority);
        mediaMuxerWrapper = new MediaMuxerWrapper(outputFile);
        audioEncoder = new AudioEncoder(mediaMuxerWrapper);
        audioRecorder = new AudioRecorder(audioEncoder);
        videoEncoder = new VideoEncoder(mediaMuxerWrapper, 480, 640);
    }

    public AudioRecorderHandlerThread(String name, int priority, String outputFile, int width, int height) {
        super(name, priority);
        mediaMuxerWrapper = new MediaMuxerWrapper(outputFile);
        audioEncoder = new AudioEncoder(mediaMuxerWrapper);
        audioRecorder = new AudioRecorder(audioEncoder);
        videoEncoder = new VideoEncoder(mediaMuxerWrapper, width, height);
    }


    public void setCallback(Handler cb) {
        mCallback = cb;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mHandler = new Handler(getLooper(), this);
    }

    @Override
    public boolean handleMessage(Message message) {
        Log.e(TAG, "handleMessage: "+message.what );
        switch (message.what) {
            case MSG_RECORDING_START:
                Log.d(TAG, "recording start message received");
                mCallback.sendMessage(Message.obtain(null, Messages.MSG_RECORDING_START_CALLBACK));
                audioRecorder.start();
                audioEncoder.start();
                videoEncoder.start();
                isStart = true;
                audioRecorder.record();
                break;
            case MSG_RECORDING_STOP:
                Log.d(TAG, "recording stop message received");
                mCallback.sendMessage(Message.obtain(null, Messages.MSG_RECORDING_STOP_CALLBACK));
                audioRecorder.stopRecording();
                videoEncoder.stop();
                isStart = false;
                break;
        }
        Log.e(TAG, "handleMessage: "+message.what +" "+isStart );
        return true;
    }

    public void startRecording() {
        Message msg = Message.obtain(null, MSG_RECORDING_START);
        mHandler.sendMessage(msg);

    }

    public void stopRecording() {
        Log.d(TAG, "here");
        audioRecorder.setIsRecordingFalse();
        Message msg = Message.obtain(null, MSG_RECORDING_STOP);
        mHandler.sendMessage(msg);
    }

    public VideoEncoder getVideoEncoder() {
        return videoEncoder;
    }
}
