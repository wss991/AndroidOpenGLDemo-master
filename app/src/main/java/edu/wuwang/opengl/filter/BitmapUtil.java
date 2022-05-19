package edu.wuwang.opengl.filter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextUtils;

import java.util.HashMap;

/**
 * Created by Android Studio.
 * User: admin
 * Date: 2022/5/17
 * Time: 15:24
 */
public final class BitmapUtil {
    public HashMap bitmapHashMap = new HashMap();

    private final void initPic() {
        this.bitmapHashMap = new HashMap();
        int i = 0;

        HashMap var10000;
        Bitmap var3;
        for (byte var2 = 9; i <= var2; ++i) {
            var10000 = this.bitmapHashMap;
            if (var10000 != null) {
                var3 = (Bitmap) var10000.put(String.valueOf(i), this.getBitmap(String.valueOf(i)));
            }
        }

        var10000 = this.bitmapHashMap;
        if (var10000 != null) {
            var3 = (Bitmap) var10000.put("-", this.getBitmap("-"));
        }

        var10000 = this.bitmapHashMap;
        if (var10000 != null) {
            var3 = (Bitmap) var10000.put(":", this.getBitmap(":"));
        }

        var10000 = this.bitmapHashMap;
        if (var10000 != null) {
            var3 = (Bitmap) var10000.put("/", this.getBitmap("/"));
        }

    }

    public final Bitmap getBitmap(String text) {
        if (!TextUtils.isEmpty((CharSequence) text)) {
            Paint paint = new Paint();
            paint.setTextSize(200.0F);
            paint.setColor(Color.parseColor("#ff0000"));
            Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
            int height = 200;
            Rect rect = new Rect();
            paint.getTextBounds(text, 0, text.length(), rect);
            //  paint.setTypeface(Typeface.DEFAULT_BOLD);
            int width = rect.width() + 10;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            canvas.drawText(text, 0.0F, -((float) metrics.ascent), paint);
            canvas.save();
            return bitmap;
        } else {
            return null;
        }
    }

    public final void release() {
        HashMap var10000 = this.bitmapHashMap;
        if (var10000 != null) {
            var10000.clear();
        }

        this.bitmapHashMap = (HashMap) null;
    }

    public final Bitmap getBitmapByTxt(String txt) {
        return (Bitmap) (bitmapHashMap.get(txt));
    }

    public BitmapUtil() {
        initPic();
    }
}
