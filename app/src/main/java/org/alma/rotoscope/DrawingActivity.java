package org.alma.rotoscope;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class DrawingActivity extends Activity {

  private static final String TAG = "DrawingActivity";
  /**
   * fps of my video
   */
  private static final float OUTPUT_FPS = 12.0f;

  /**
   * fps of original video
   */
  private static final float INPUT_FPS = 24.0f;

  private static final float RATE_FPS = INPUT_FPS / OUTPUT_FPS;

  private MediaMetadataRetriever metadata;
  private int currentPicture;
  private int nbImageInput;

  private SparseArray<Bitmap> layers;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    { // load metadata
      Bundle bundle = getIntent().getExtras();
      String resourcePath = bundle.getString("resourcePath");

      Log.d(TAG, "resource=" + resourcePath);

      metadata = new MediaMetadataRetriever();
      metadata.setDataSource(this, Uri.parse(resourcePath));

      nbImageInput = (int)(Float.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000 * INPUT_FPS);
      Log.d(TAG, "nbImageInput=" + nbImageInput);
      Log.d(TAG, "RATE_FPS=" + RATE_FPS);
    }

    int width = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
    int height = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

    { // find screen orientation

      int rotation = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));

      Log.d(TAG, "width=" + width);
      Log.d(TAG, "height=" + height);
      Log.d(TAG, "rotation=" + rotation);


      if ((width < height && (rotation == 0 || rotation == 180))
          || (width > height && (rotation == 90 || rotation == 270))) {
        // portrait
        Log.d(TAG, "screenRotation to portrait");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      } else { // landscape
        Log.d(TAG, "screenRotation to landscape");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      }

    }

    { // set view
      int nbImageOutput = (int) (nbImageInput / RATE_FPS);
      Log.d(TAG, "nbImageOutput=" + nbImageOutput);

      layers = new SparseArray<>(nbImageOutput);
      Point size = new Point();
      getWindowManager().getDefaultDisplay().getSize(size);
      Log.d(TAG, "size : " + size.x + "x" + size.y);
      for (int i = 0; i < nbImageOutput; ++i) {
        layers.put(i, Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888));
      }

      setContentView(R.layout.activity_drawing);

      currentPicture = 0;
      setLayer();
    }
  }

  private void setLayer() {

    DrawingArea drawingArea = (DrawingArea) findViewById(R.id.drawingAreaView);

    long time = (long) (RATE_FPS * currentPicture * 1000000 / OUTPUT_FPS);

    Log.d(TAG, "show picture " + currentPicture);
    Log.d(TAG, "show Frame at " + time);

    //metadata.setDataSource(this, Uri.parse(resourcePath));
    Bitmap background = metadata.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST);
    drawingArea.setLayer(layers.get(currentPicture), new BitmapDrawable(getResources(), background));
  }

  public void nextPicture(View view) {
    if ((currentPicture+1) > nbImageInput * RATE_FPS ) {
      return;
    }

    currentPicture++;
    setLayer();
  }

  public void previousPicture(View view) {
    if (currentPicture == 0) {
      return;
    }

    currentPicture--;
    setLayer();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
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


}
