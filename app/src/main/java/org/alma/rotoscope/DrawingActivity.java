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
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import org.alma.rotoscope.colorpicker.ColorPickerDialog;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * This activity create new full screen window to do a rotoscope a video.
 *
 * @see DrawingArea
 * @author dralagen
 */
public class DrawingActivity extends Activity implements View.OnTouchListener {

  private static final String TAG = "DrawingActivity";

  private static final Handler handler = new Handler();

  /**
   * fps of original video
   */
  private float inputFps;

  /**
   * Rate between inputFps / outputFps
   */
  private float rateFps;

  /**
   * Storage the meta of video
   */
  private MediaMetadataRetriever metadata;

  /**
   * number of current picture show
   */
  private int currentPicture;

  /**
   * All layers will drawn
   */
  private List<Bitmap> layers;

  /**
   * A color picker dialog to change the paint color
   */
  private ColorPickerDialog colorPicker;

  /**
   * Runnable function to hide menu after timer
   */
  private Runnable runHideMenu;

  /**
   * true if you don't have MotionEvent.ACTION_MOVE
   */
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

    { // find screen orientation

      int videoWidth = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
      int videoHeight = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
      int videoRotation = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));

      Log.d(TAG, "videoWidth=" + videoWidth);
      Log.d(TAG, "videoHeight=" + videoHeight);
      Log.d(TAG, "videoRotation=" + videoRotation);


      if ((videoWidth < videoHeight && (videoRotation == 0 || videoRotation == 180))
          || (videoWidth > videoHeight && (videoRotation == 90 || videoRotation == 270))) {
        // portrait
        Log.d(TAG, "screenRotation to portrait");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      } else { // landscape
        Log.d(TAG, "screenRotation to landscape");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      }

    }

    { // set view

      // get screen size
      Point size = new Point();
      getWindowManager().getDefaultDisplay().getSize(size);
      Log.d(TAG, "size : " + size.x + "x" + size.y);

      // create all final bitmap
      int nbImageOutput = (int) (nbImageInput / rateFps);
      Log.d(TAG, "nbImageOutput=" + nbImageOutput);
      layers = new ArrayList<>(nbImageOutput);
      for (int i = 0; i < nbImageOutput; ++i) {
        layers.add(Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888));
      }

      setContentView(R.layout.activity_drawing);

      // setup drawingArea
      final DrawingArea drawingArea = (DrawingArea) findViewById(R.id.drawingAreaView);
      drawingArea.setOnTouchListener(this);
      currentPicture = 0;
      setLayer();

      // set color picker
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

      // hidden all menu
      final View menu = findViewById(R.id.MenuLayout);
      menu.setVisibility(View.INVISIBLE);
      final View nav = findViewById(R.id.navigationLayout);
      nav.setVisibility(View.INVISIBLE);
    }
  }

  /**
   * Select the currentPicture into layers and the good frame form the input video,
   * and pass all at the DrawingArea and use them to drawing into current picture and show good frame.
   */
  private void setLayer() {

    DrawingArea drawingArea = (DrawingArea) findViewById(R.id.drawingAreaView);

    // calculate the good time for a specific frame correspond currentPicture into video
    long time = (long) (rateFps * (currentPicture * 1000000 / inputFps));

    Log.d(TAG, "show picture " + currentPicture);
    Log.d(TAG, "show Frame at " + time);

    Bitmap background = metadata.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST);
    drawingArea.setLayer(layers.get(currentPicture), new BitmapDrawable(getResources(), background));

    findViewById(R.id.PreviousButton).setVisibility((currentPicture != 0) ? View.VISIBLE : View.INVISIBLE);
    findViewById(R.id.NextButton).setVisibility((currentPicture < layers.size() - 1) ? View.VISIBLE : View.INVISIBLE);
  }

  /**
   * Pass to the next picture if exist and call setLayer
   * @param view android view
   */
  public void nextPicture(View view) {
    if ((currentPicture+1) >= layers.size() ) {
      return;
    }

    currentPicture++;
    setLayer();
  }

  /**
   * Pass to the previous picture if exist and call setLayer
   * @param view android view
   */
  public void previousPicture(View view) {
    if (currentPicture == 0) {
      return;
    }

    currentPicture--;
    setLayer();
  }

  /**
   * Reset the current picture and call setLayer
   * @param view android view
   */
  public void erasePicture(View view) {
    Point size = new Point();
    getWindowManager().getDefaultDisplay().getSize(size);
    layers.add(currentPicture, Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888));
    setLayer();
  }

  /**
   * Assemble all picture drawn on layers to one video on external storage in directory movies.
   *
   * TODO dralagen 4/4/15 : storage video in directory getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), String.valueOf(R.string.app_name)
   * @param view android view
   */
  public void saveVideo(View view) {
    String outputVideoName = new SimpleDateFormat("yyyyMMdd_HHmmss",
        Locale.getDefault())
        .format(new Date());

    File outputVideo = new File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_MOVIES),
        outputVideoName + ".mp4"
    );

    Log.d(TAG, "outputVideo=" + outputVideo.getAbsolutePath());

    try {
      SequenceEncoder encoder = new SequenceEncoder(outputVideo);
      for (Bitmap frame : layers) {
        encoder.encodeNativeFrame(fromBitmap(frame));
      }
      encoder.finish();

      Toast.makeText(this, "Video is finish on " + outputVideo.getAbsolutePath(),
          Toast.LENGTH_LONG)
          .show();
    } catch (IOException e) {
      e.printStackTrace();
      Toast.makeText(this, "Error when try to encode all picture in video",
          Toast.LENGTH_LONG)
          .show();

    }
  }

  /**
   * Convert a Bitmap to a Picture
   * @param frame Bitmap to convert
   * @return the convert result
   */
  private Picture fromBitmap(Bitmap frame) {
    Picture dst = Picture.create(frame.getWidth(), frame.getHeight(), ColorSpace.RGB);

    int[] dstData = dst.getPlaneData(0);
    int[] packed = new int[frame.getWidth() * frame.getHeight()];

    frame.getPixels(packed, 0, frame.getWidth(), 0, 0, frame.getWidth(), frame.getHeight());

    for (int i = 0, frameOff = 0, dstOff = 0; i < frame.getHeight(); i++) {
      for (int j = 0; j < frame.getWidth(); j++, frameOff++, dstOff += 3) {
        int rgb = packed[frameOff];
        dstData[dstOff]     = (rgb >> 16) & 0xff;
        dstData[dstOff + 1] = (rgb >> 8) & 0xff;
        dstData[dstOff + 2] = rgb & 0xff;
      }
    }
    
    return dst;
  }

  /**
   * Take the result of saveVideo and share it.
   *
   * TODO dralagen 4/4/15 : share video result
   * @param view android view
   */
  public void shareVideo(View view) {
  }

  /**
   * Show the color picker of the paint
   * @param view android view
   */
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

  /**
   * Animate view 
   * @param v view will animate
   * @param fadeIn true if become visible, false become invisible
   */
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
