package org.alma.rotoscope;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class DrawingActivity extends Activity {

  private static final String TAG = "DrawingActivity";
  /**
   * fps of my video
   */
  private static final int OUTPUT_FPS = 6;

  /**
   * fps of original video
   */
  private static final int INPUT_FPS = 24;

  private String resourcePath;
  private MediaMetadataRetriever metadata;
  private int currentPicture;
  private int duration;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    { // load metadata
      Bundle bundle = getIntent().getExtras();
      resourcePath = bundle.getString("resourcePath");

      Log.d(TAG, "resource=" + resourcePath);

      metadata = new MediaMetadataRetriever();
      metadata.setDataSource(this, Uri.parse(resourcePath));

      setContentView(R.layout.activity_drawing);
    }

    { // find screen orientation
      int width = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
      int height = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
      int rotation = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
      duration = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

      Log.d(TAG, "width=" + width);
      Log.d(TAG, "height=" + height);
      Log.d(TAG, "rotation=" + rotation);
      Log.d(TAG, "duration=" + duration);
      Log.d(TAG, "maxImage=" + (int)((float) duration / 1000) * ((float) OUTPUT_FPS / INPUT_FPS));


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

    { // set background
      currentPicture = 0;
      setBackground();
    }
  }

  private void setBackground() {

    View drawingArea = findViewById(R.id.drawingAreaView);

    long time = currentPicture * 100000 * INPUT_FPS / OUTPUT_FPS;

    Log.d(TAG, "show picture " + currentPicture);
    Log.d(TAG, "show Frame at " + time);

    //metadata.setDataSource(this, Uri.parse(resourcePath));
    Bitmap background = metadata.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST);
    drawingArea.setBackground(new BitmapDrawable(getResources(), background));
  }

  public void nextPicture(View view) {
    if (currentPicture >= duration * OUTPUT_FPS / INPUT_FPS) {
      return;
    }

    currentPicture++;
    setBackground();
  }

  public void previousPicture(View view) {
    if (currentPicture == 0) {
      return;
    }

    currentPicture--;
    setBackground();
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
