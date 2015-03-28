package org.alma.rotoscope;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


public class DrawingActivity extends Activity {

  private Paint mPaint;
  private String resourcePath;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle bundle = getIntent().getExtras();
    resourcePath = bundle.getString("resourcePath");

    Toast.makeText(this, "resourcePath : " + resourcePath, Toast.LENGTH_LONG).show();

    ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }

    DrawingAreaView drawingAreaView = new DrawingAreaView(this);

    setContentView(drawingAreaView);


    mPaint = new Paint();
    mPaint.setAntiAlias(true);
    mPaint.setDither(true);
    mPaint.setColor(Color.WHITE);
    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setStrokeJoin(Paint.Join.ROUND);
    mPaint.setStrokeCap(Paint.Cap.ROUND);
    mPaint.setStrokeWidth(12);
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_drawing, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public class DrawingAreaView extends View {

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint   mBitmapPaint;
    Context context;

    public DrawingAreaView(Context context) {
      super(context);
      this.context = context;
      mPath = new Path();
      mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
      super.onSizeChanged(w, h, oldw, oldh);

      mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
      mCanvas = new Canvas(mBitmap);

    }
    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);

      canvas.drawBitmap( mBitmap, 0, 0, mBitmapPaint);

      canvas.drawPath( mPath,  mPaint);

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

}
