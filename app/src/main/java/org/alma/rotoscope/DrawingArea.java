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

  private Bitmap mBitmap;
  private Canvas mCanvas;
  private Path mPath;
  private Paint mBitmapPaint;
  private Paint mPaint;
  Context context;

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
    mPath = new Path();
    mBitmapPaint = new Paint(Paint.DITHER_FLAG);

    mPaint = new Paint();
    mPaint.setAntiAlias(true);
    mPaint.setDither(true);
    mPaint.setColor(Color.WHITE);
    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setStrokeJoin(Paint.Join.ROUND);
    mPaint.setStrokeCap(Paint.Cap.ROUND);
    mPaint.setStrokeWidth(12);

  }

  public void setLayer(Bitmap bitmap, BitmapDrawable bitmapDrawable) {
    mBitmap = bitmap;
    setBackground(bitmapDrawable);
  }

  @Override
  public void setBackground(Drawable background) {
    super.setBackground(background);

    if (mBitmap != null) {
      mCanvas = new Canvas(mBitmap);
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    Log.d(TAG, "Width  : " + w);
    Log.d(TAG, "Height : " + h);

    if (mBitmap != null) {
      mCanvas = new Canvas(mBitmap);
    }

  }
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (mBitmap != null ) {
      canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }

    canvas.drawPath( mPath,  mPaint);

  }

  public int getColor() {
    return mPaint.getColor();
  }

  public void setColor(int color) {
    mPaint.setColor(color);
  }

  private float mX, mY;
  private static final float TOUCH_TOLERANCE = 4;

  private void touch_start(float x, float y) {
    mPath.reset();
    mPath.moveTo(x, y);
    mX = x;
    mY = y;
  }
  private void touch_move(float x, float y) {
    float dx = Math.abs(x - mX);
    float dy = Math.abs(y - mY);
    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
      mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
      mX = x;
      mY = y;

    }
  }
  private void touch_up() {
    mPath.lineTo(mX, mY);
    // commit the path to our offscreen
    mCanvas.drawPath(mPath,  mPaint);
    // kill this so we don't double draw
    mPath.reset();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();

    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        touch_start(x, y);
        invalidate();
        break;
      case MotionEvent.ACTION_MOVE:
        touch_move(x, y);
        invalidate();
        break;
      case MotionEvent.ACTION_UP:
        touch_up();
        invalidate();
        break;
    }
    return true;
  }
}
