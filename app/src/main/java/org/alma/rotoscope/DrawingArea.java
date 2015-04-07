package org.alma.rotoscope;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * This view create an drawing area only, no button or option are create.
 *
 * @author dralagen
 */
public class DrawingArea extends View {

  private static final String TAG = "DrawingArea";
  private static final float TOUCH_TOLERANCE = 4;

  /**
   * contain the final result of my layer
   */
  private Bitmap bitmap;

  /**
   * workspace to drawing
   */
  private Canvas canvas;

  /**
   * helper drawing
   */
  private Path path;

  /**
   * draw paint on bitmap
   */
  private Paint bitmapPaint;

  /**
   * draw paint on canvas
   */
  private Paint paint;

  /**
   * start position X of finger when start drawing
   */
  private float touchX;
  /**
   * start position X of finger when start drawing
   */
  private float touchY;

  public DrawingArea(Context context) {
    super(context);
    setupDrawingArea();
  }

  public DrawingArea(Context context, AttributeSet attrs) {
    super(context, attrs);
    setupDrawingArea();
  }

  public DrawingArea(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setupDrawingArea();
  }

  /**
   * Setup default paint options
   */
  public void setupDrawingArea() {
    //this.context = context;
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

  /**
   * Set new layer and draw on bitmap
   *
   * @param bitmap contain the final layer result
   */
  public void setLayer(Bitmap bitmap) {
    this.bitmap = bitmap;
    if (bitmap != null) {
      canvas = new Canvas(bitmap);
    }
    invalidate();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    Log.v(TAG, "Width  : " + w);
    Log.v(TAG, "Height : " + h);

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

  /**
   * Get color of current paint
   * @return color of current paint
   */
  public int getColor() {
    return paint.getColor();
  }

  /**
   * Set color of current paint
   * @param color color of new paint
   */
  public void setColor(int color) {
    paint.setColor(color);
  }

  /**
   * Start drawing to the position (x,y)
   * @param x position x of finger
   * @param y position y of finger
   */
  private void onTouchStart(float x, float y) {
    path.reset();
    path.moveTo(x, y);
    touchX = x;
    touchY = y;
  }

  /**
   * Draw line if the gesture respect the TOUCH_TOLERANCE
   *
   * @param x position x of finger
   * @param y position y of finger
   */
  private void onTouchMove(float x, float y) {
    float dx = Math.abs(x - touchX);
    float dy = Math.abs(y - touchY);
    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
      path.quadTo(touchX, touchY, (x + touchX) / 2, (y + touchY) / 2);
      touchX = x;
      touchY = y;

    }
  }

  /**
   * End of gesture, commit the draw
   */
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
