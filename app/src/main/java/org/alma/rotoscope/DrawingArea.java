package org.alma.rotoscope;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created on 3/29/15.
 *
 * @author dralagen
 */
public class DrawingArea extends View {

  private static final String TAG = "DrawingArea";
  private static final float TOUCH_TOLERANCE = 4;

  Context context;
  private Bitmap bitmap;
  private Canvas canvas;
  private Path path;
  private Paint bitmapPaint;
  private Paint paint;
  private float touchX, touchY;

  public DrawingArea(Context context) {
    super(context);
    setupDrawingArea(context);
  }

  public DrawingArea(Context context, AttributeSet attrs) {
    super(context, attrs);
    setupDrawingArea(context);
  }

  public DrawingArea(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setupDrawingArea(context);
  }

  public void setupDrawingArea(Context context) {
    this.context = context;
    path = new Path();
    bitmapPaint = new Paint(Paint.DITHER_FLAG);

    paint = new Paint();
    paint.setAntiAlias(true);
    paint.setDither(true);
    paint.setColor(Color.WHITE);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeJoin(Paint.Join.ROUND);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setStrokeWidth(12);

  }

  public void setLayer(Bitmap bitmap, BitmapDrawable bitmapDrawable) {
    this.bitmap = bitmap;
    setBackground(bitmapDrawable);
  }

  @Override
  public void setBackground(Drawable background) {
    super.setBackground(background);

    if (bitmap != null) {
      canvas = new Canvas(bitmap);
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    Log.d(TAG, "Width  : " + w);
    Log.d(TAG, "Height : " + h);

    if (bitmap != null) {
      canvas = new Canvas(bitmap);
    }

  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (bitmap != null ) {
      canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
    }

    canvas.drawPath(path, paint);

  }

  public int getColor() {
    return paint.getColor();
  }

  public void setColor(int color) {
    paint.setColor(color);
  }

  private void onTouchStart(float x, float y) {
    path.reset();
    path.moveTo(x, y);
    touchX = x;
    touchY = y;
  }
  private void onTouchMove(float x, float y) {
    float dx = Math.abs(x - touchX);
    float dy = Math.abs(y - touchY);
    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
      path.quadTo(touchX, touchY, (x + touchX) / 2, (y + touchY) / 2);
      touchX = x;
      touchY = y;

    }
  }
  private void onTouchFinish () {
    path.lineTo(touchX, touchY);
    // commit the path to our offscreen
    canvas.drawPath(path, paint);
    // kill this so we don't double draw
    path.reset();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();

    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        onTouchStart(x, y);
        break;
      case MotionEvent.ACTION_MOVE:
        onTouchMove(x, y);
        invalidate();
        break;
      case MotionEvent.ACTION_UP:
        onTouchFinish();
        invalidate();
        break;
    }
    return true;
  }
}
