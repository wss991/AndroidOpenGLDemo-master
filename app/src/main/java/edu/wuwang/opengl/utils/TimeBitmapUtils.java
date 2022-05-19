package edu.wuwang.opengl.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

/**
 * @description:
 * @author: wj
 * @date :   2022/5/18 15:34
 */
public class TimeBitmapUtils {
    private HashMap<String, Bitmap> hashMap = new HashMap<>();
    private Paint mPaint = new Paint();
    private Paint.FontMetrics metrics;
    private float offset; // 偏移量

    public TimeBitmapUtils() {
        initPaint();
        initBitmap();
        matrix = new Matrix();
        //  matrix.postScale(-1, -1);
    }

    private Matrix matrix;


    /*

     */
    private void initPaint() {
        mPaint.setColor(Color.YELLOW);
        mPaint.setTextSize(50f);
        metrics = mPaint.getFontMetrics();
        offset = (metrics.descent - metrics.ascent) / 2 - metrics.bottom;
    }

    private void initBitmap() {
        String key = "";
        for (int i = 0; i < 10; i++) {
            key = String.valueOf(i);
            hashMap.put(key, createBitmap(key));
        }

        hashMap.put("-", createBitmap("-"));
        hashMap.put(":", createBitmap(":"));
        hashMap.put("/", createBitmap("/"));
    }

    private static final String TAG = "TimeBitmapUtils";

    public Bitmap createBitmap(String text) {
        if (TextUtils.isEmpty(text)) {
            text = " ";
        }
        Rect rect = new Rect();
        mPaint.getTextBounds(text, 0, text.length(), rect);
        int height = 200;
        Log.e(TAG, text+"  createBitmap: "+rect.width());
        Bitmap bitmap = Bitmap.createBitmap(rect.width()+10, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(text, 5,height/2+offset, mPaint);
        canvas.save();

        return bitmap;

    }

    public Bitmap getBitmapByText(String text) {
        return hashMap.get(text);
    }

}
