package org.alma.rotoscope;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import org.alma.rotoscope.colorpicker.ColorPickerDialog;



public class DrawingActivity extends Activity implements View.OnTouchListener {

  private static final String TAG = "DrawingActivity";

  private static final Handler handler = new Handler();

  /**
   * fps of original video
   */
  private float inputFps;

  private float rateFps;

  private MediaMetadataRetriever metadata;
  private int currentPicture;

  private SparseArray<Bitmap> layers;

  private ColorPickerDialog colorPicker;

  private Runnable runHideMenu;
  private boolean shortPress;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    int nbImageInput;
    { // load metadata

      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

      float outputFps = Float.parseFloat(preferences.getString("outputVideoFrameRate", "6.0f"));
      inputFps = Float.parseFloat(preferences.getString("inputVideoFrameRate", "24.0f"));
      rateFps = inputFps / outputFps;

      Bundle bundle = getIntent().getExtras();
      String resourcePath = bundle.getString("resourcePath");

      Log.d(TAG, "resource=" + resourcePath);

      metadata = new MediaMetadataRetriever();
      metadata.setDataSource(this, Uri.parse(resourcePath));

      nbImageInput = (int)(Float.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000 * inputFps);
      Log.d(TAG, "nbImageInput=" + nbImageInput);
      Log.d(TAG, "rateFps=" + rateFps);
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
      int nbImageOutput = (int) (nbImageInput / rateFps);
      Log.d(TAG, "nbImageOutput=" + nbImageOutput);

      layers = new SparseArray<>(nbImageOutput);
      Point size = new Point();
      getWindowManager().getDefaultDisplay().getSize(size);
      Log.d(TAG, "size : " + size.x + "x" + size.y);
      for (int i = 0; i < nbImageOutput; ++i) {
        layers.put(i, Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888));
      }

      setContentView(R.layout.activity_drawing);
      final DrawingArea drawingArea = (DrawingArea) findViewById(R.id.drawingAreaView);
      drawingArea.setOnTouchListener(this);
      currentPicture = 0;
      setLayer();

      colorPicker = new ColorPickerDialog(this, drawingArea.getColor());
      colorPicker.setAlphaSliderVisible(true);
      colorPicker.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
        @Override
        public void onClick (DialogInterface dialog, int which) {
          drawingArea.setColor(colorPicker.getColor());
        }
      });
      colorPicker.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
        }
      });

      final View menu = findViewById(R.id.MenuLayout);
      menu.setVisibility(View.INVISIBLE);
      final View nav = findViewById(R.id.navigationLayout);
      nav.setVisibility(View.INVISIBLE);
    }
  }

  private void setLayer() {

    DrawingArea drawingArea = (DrawingArea) findViewById(R.id.drawingAreaView);

    long time = (long) (rateFps * (currentPicture * 1000000 / inputFps));

    Log.d(TAG, "show picture " + currentPicture);
    Log.d(TAG, "show Frame at " + time);

    //metadata.setDataSource(this, Uri.parse(resourcePath));
    Bitmap background = metadata.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST);
    drawingArea.setLayer(layers.get(currentPicture), new BitmapDrawable(getResources(), background));

    findViewById(R.id.PreviousButton).setEnabled(currentPicture != 0);
    findViewById(R.id.NextButton).setEnabled(currentPicture < layers.size() - 1);
  }

  public void nextPicture(View view) {
    if ((currentPicture+1) >= layers.size() ) {
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

  public void erasePicture(View view) {
    Point size = new Point();
    getWindowManager().getDefaultDisplay().getSize(size);
    layers.put(currentPicture, Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888));
    setLayer();
  }

  public void pickColor(View view) {
    colorPicker.show();
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    switch (event.getAction()) {
      case  MotionEvent.ACTION_DOWN:
        onTouchDown();
        break;

      case MotionEvent.ACTION_MOVE:
        onTouchMove();
        break;

      case MotionEvent.ACTION_UP:
        onTouchUp();
    }
    return false;
  }

  private void onTouchDown() {
    shortPress = true;
  }

  private void onTouchMove() {
    shortPress = false;
    handler.removeCallbacks(runHideMenu);
    handler.post(runHideMenu);
    runHideMenu = null;
  }

  private void onTouchUp() {
    if (shortPress) {

      final View menu = findViewById(R.id.MenuLayout);
      fade(menu, true);

      final View nav = findViewById(R.id.navigationLayout);
      fade(nav, true);

      Log.d(TAG, "Menu visible");

      runHideMenu = new Runnable() {
        @Override
        public void run() {
          fade(menu, false);
          fade(nav, false);
          Log.d(TAG, "Menu invisible");
        }
      };
      handler.postDelayed(runHideMenu, 5000);
    }
  }

  private void fade(final View v, boolean fadeIn) {

    if (fadeIn && v.getVisibility() != View.VISIBLE) {
      v.setAlpha(0f);
      v.setVisibility(View.VISIBLE);

      v.animate()
          .alpha(1f)
          .setDuration(200)
          .setListener(null);

    } else if (!fadeIn && v.getVisibility() == View.VISIBLE) {
      v.animate()
          .alpha(0f)
          .setDuration(800)
          .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
              v.setVisibility(View.GONE);
            }
          });
    }
  }
}
