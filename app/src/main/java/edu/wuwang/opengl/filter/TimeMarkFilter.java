/*
 *
 * WaterMarkFilter.java
 * 
 * Created by Wuwang on 2016/12/15
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package edu.wuwang.opengl.filter;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import edu.wuwang.opengl.utils.MatrixUtils;

/**
 * 水印Filter示例
 */
public class TimeMarkFilter extends NoFilter {

    protected Bitmap mBitmap;
    protected NoFilter mFilter;
    protected int width,height;

    protected int x,y,w,h;

    public TimeMarkFilter(Resources mRes) {
        super(mRes);
        mFilter=new NoFilter(mRes){
            @Override
            protected void onClear() {

            }
        };
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mFilter.create();
        createTexture();
    }

    @Override
    protected void onClear() {
        super.onClear();
    }

    boolean a ;
    @Override
    public void draw() {
        super.draw();
        Log.i("WaterMarkFilter","每一帧");
        String now = timeFormat.format(new Date());
        if (!lastTime.equals(now)){
            lastTime = now;
            this.mBitmap = bitmapUtil.getBitmap(timeFormat.format(new Date()));
            createTexture();
        }
        GLES20.glViewport(x,y,w==0?mBitmap.getWidth():w,h==0?mBitmap.getHeight():h);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR,GLES20.GL_DST_ALPHA);
        mFilter.draw();
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glViewport(0,0,width,height);
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        this.width=width;
        this.height=height;
        mFilter.setSize(width,height);
    }

    private BitmapUtil bitmapUtil = new BitmapUtil();
    private SimpleDateFormat timeFormat= new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

    String lastTime="";
    public void setWaterMark(Bitmap bitmap){
        if(this.mBitmap!=null){
            this.mBitmap.recycle();
        }
        this.mBitmap = bitmapUtil.getBitmap(timeFormat.format(new Date()));
//        this.mBitmap=bitmap;
    }

    private int[] textures=new int[1];
    protected void createTexture(){
        if(mBitmap!=null){
            //生成纹理
            GLES20.glGenTextures(1,textures,0);
            //生成纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[0]);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);

            if (matrixed){
                MatrixUtils.flip(mFilter.getMatrix(),false,true);
                matrixed=false;
            }

            mFilter.setTextureId(textures[0]);
        }
    }
    boolean matrixed=true;

    public void setPosition(int x,int y,int width,int height){
        this.x=x;
        this.y=y;
//        this.w=width;
//        this.h=height;
        this.w = mBitmap.getWidth();
        this.h = mBitmap.getHeight();
    }

}
