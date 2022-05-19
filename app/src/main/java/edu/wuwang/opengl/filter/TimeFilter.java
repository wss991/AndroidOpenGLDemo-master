package edu.wuwang.opengl.filter;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import edu.wuwang.opengl.utils.MatrixUtils;
import edu.wuwang.opengl.utils.TimeBitmapUtils;

/**
 * @description: 动态时间 水印
 * @author: wj
 * @date :   2022/5/18 15:01
 */
public class TimeFilter extends NoFilter {


    private NoFilter mFilter;
    private int width, height;

    private int x, y, w, h;
    private TimeBitmapUtils timeBitmapUtils;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
    private HashMap<Bitmap, Integer> hashMap = new HashMap<>();

    public TimeFilter(Resources mRes) {
        super(mRes);
        mFilter = new NoFilter(mRes) {
            @Override
            protected void onClear() {

            }
        };
        timeBitmapUtils = new TimeBitmapUtils();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mFilter.create();
        MatrixUtils.flip(mFilter.getMatrix(), false, true);
    }

    @Override
    protected void onClear() {
        super.onClear();
    }

    @Override
    public void draw() {
        super.draw();
        drawTime();

    }

    private static final String TAG = "testjin";

    private void drawTime() {
        String dateStr = timeFormat.format(new Date());
        Bitmap bitmap = null;
        int marginLeft = x;
        Log.e(TAG, "drawTime: " + marginLeft);
        for (int i = 0; i < dateStr.length(); i++) { // 此处 可以换成一张图片，所有文字画在一起
            bitmap = timeBitmapUtils.getBitmapByText(((Character) dateStr.charAt(i)).toString());
            if (bitmap != null) {
                GLES20.glViewport(marginLeft, y, bitmap.getWidth(), bitmap.getHeight());
                GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                createTexture(bitmap);
                marginLeft = marginLeft + bitmap.getWidth();

            }

        }
    }


    @Override
    protected void onSizeChanged(int width, int height) {
        this.width = width;
        this.height = height;
        mFilter.setSize(width, height);
    }


    private int[] textures = new int[1];

    private void createTexture(Bitmap mBitmap) {
        if (mBitmap != null) {
            Integer textureID = hashMap.get(mBitmap);
            if (textureID == null) {
                //生成纹理
                textures = new int[1];
                GLES20.glGenTextures(1, textures, 0);
                //生成纹理
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
                //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
                textureID = textures[0];
                hashMap.put(mBitmap, textureID);
            }

            mFilter.setTextureId(textureID);
            mFilter.draw();
            GLES20.glDisable(GLES20.GL_BLEND);
            GLES20.glViewport(0, 0, width, height);

            //  MatrixUtils.flip(mFilter.getMatrix(),false,true);


        }
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;

    }


    public void setWaterMark(Bitmap bitmap) {

    }


    public void setPosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.w = width;
        this.h = height;
    }
}
