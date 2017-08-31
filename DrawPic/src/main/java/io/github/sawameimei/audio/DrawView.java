package io.github.sawameimei.audio;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by huangmeng on 2017/8/31.
 */

public class DrawView extends View {

    private Integer resId;

    public DrawView(Context context) {
        super(context);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setResId(Integer resId) {
        this.resId = resId;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (resId != null) {
            Resources resources = getContext().getResources();
            Bitmap bitmap = BitmapFactory.decodeResource(resources, resId);
            canvas.drawBitmap(bitmap, 0, 0, new Paint());
        }
    }
}
