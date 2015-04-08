package org.alma.rotoscope;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import org.alma.rotoscope.colorpicker.ColorPickerDialog;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


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
   * Contain the current frame of video
   */
  private Bitmap currentFrame;

  /**
   * Cache of video frame
   */
  private final List<Bitmap> cache = Collections.synchronizedList(new ArrayList<Bitmap>());

  /**
   * All layers will drawn
   */
  private List<Bitmap> layers;

  /**
   * Number of onion skin
   */
  private int nbOnion;

  /**
   * Frequency of onion skin
   */
  private int freqOnion;

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

  /**
   * File output of result video
   */
  private File outputVideo;

  private boolean cacheUpToDate;
  /**
   * Runnable to invalid and create cache of video
   */
  private final Runnable invalidCacheRunnable = new Runnable() {
    @Override
    public void run() {
      while (!cacheUpToDate) {
        cacheUpToDate = true;

        // index of min cache picture
        int minCache = currentPicture - 3;
        // index of max cache picture
        int maxCache = currentPicture + 5;

        // loop take more large of [minCache,maxCache] to free memory
        for (int i = Math.max(minCache - 3, 0); i < Math.min(maxCache + 5, layers.size()); ++i) {
          if (minCache <= i && i <= maxCache) { // cache
            if (cache.get(i) == null) { // if not exist
              cache.set(i, getFrameVideo(i));
              Log.v(TAG, "cache add frame " + i);
            }
          } else { // free
            cache.set(i, null);
          }
        }
      }
      Log.v(TAG, "Caculate cache terminate");
    }
  };

  /**
   * Thread to recalculate the cache frames of input video
   */
  private Thread invalidCacheThread;

  /**
   * True to show the input video frame to background of DrawingArea
   */
  private boolean showBackground;

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
      outputVideo = null;

      Log.v(TAG, "resource=" + resourcePath);

      metadata = new MediaMetadataRetriever();
      metadata.setDataSource(this, Uri.parse(resourcePath));

      nbImageInput = (int)(Float.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000 * inputFps);
      Log.v(TAG, "nbImageInput=" + nbImageInput);
      Log.v(TAG, "rateFps=" + rateFps);
    }

    { // find screen orientation

      int videoWidth = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
      int videoHeight = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
      int videoRotation = Integer.valueOf(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));

      Log.v(TAG, "videoWidth=" + videoWidth);
      Log.v(TAG, "videoHeight=" + videoHeight);
      Log.v(TAG, "videoRotation=" + videoRotation);


      if ((videoWidth < videoHeight && (videoRotation == 0 || videoRotation == 180))
          || (videoWidth > videoHeight && (videoRotation == 90 || videoRotation == 270))) {
        // portrait
        Log.v(TAG, "screenRotation to portrait");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      } else { // landscape
        Log.v(TAG, "screenRotation to landscape");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      }

    }

    { // set view

      // get screen size
      Point size = new Point();
      getWindowManager().getDefaultDisplay().getSize(size);
      Log.v(TAG, "size : " + size.x + "x" + size.y);

      // create all final bitmap
      int nbImageOutput = (int) (nbImageInput / rateFps);
      Log.v(TAG, "nbImageOutput=" + nbImageOutput);
      layers = new ArrayList<>(nbImageOutput);
      for (int i = 0; i < nbImageOutput; ++i) {
        layers.add(Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_4444));
        cache.add(null);
      }

      invalidCacheThread = new Thread();

      setContentView(R.layout.activity_drawing);

      // setup drawingArea
      final DrawingArea drawingArea = (DrawingArea) findViewById(R.id.drawingAreaView);
      drawingArea.setOnTouchListener(this);
      currentPicture = 0;
      showBackground = true;
      nbOnion = 0;
      freqOnion = 1;

      setLayer();

      // set color picker
      colorPicker = new ColorPickerDialog(this, drawingArea.getColor());
      colorPicker.setAlphaSliderVisible(true);
      colorPicker.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          drawingArea.setColor(colorPicker.getColor());
        }
      });
      colorPicker.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
        }
      });
    }
  }

  /**
   * Select the currentPicture into layers and the good frame form the input video,
   * and pass all at the DrawingArea and use them to drawing into current picture and show good frame.
   */
  private void setLayer() {

    final ProgressDialog loadVideoProgress = new ProgressDialog(this);
    loadVideoProgress.setMessage("Load new Frame");
    loadVideoProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    loadVideoProgress.setIndeterminate(true);
    loadVideoProgress.setCancelable(false);

    Log.v(TAG, "show picture " + currentPicture);

    if (cache.get(currentPicture) != null) {
      currentFrame = cache.get(currentPicture);
      refreshDrawingArea();

    } else {
      // Load new frame
      loadVideoProgress.show();
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            currentFrame = getFrameVideo(currentPicture);

            handler.post(new Runnable() {
              @Override
              public void run() {
                refreshDrawingArea();
                synchronized (cache) {
                  cache.set(currentPicture, currentFrame);
                }
              }
            });
          } finally {
            loadVideoProgress.dismiss();
          }
        }
      }).start();

    }
  }

  /**
   * Send some action as refresh background and layer to DrawingArea,
   * Update numFrameLabel,
   * Set visible or invisible if can or not go to next picture or previous picture.
   */
  private void refreshDrawingArea() {
    DrawingArea drawingArea = (DrawingArea) findViewById(R.id.drawingAreaView);
    drawingArea.setLayer(layers.get(currentPicture));
    if (showBackground) {
      Bitmap background = Bitmap.createBitmap(currentFrame);
      Canvas canvasBackground = new Canvas(background);
      Paint backgroundPaint = new Paint(Paint.DITHER_FLAG);
      backgroundPaint.setAlpha(100);

      // Draw onion skin on background
      int firstIndex = Math.max(currentPicture - nbOnion*freqOnion, 0);

      for (int i = currentPicture - freqOnion ; i >= firstIndex; i-=freqOnion) {
        // opacity between [100,200]
        backgroundPaint.setAlpha((100/nbOnion*freqOnion)*(i-firstIndex) + 100);
        canvasBackground.drawBitmap(layers.get(i), 0, 0, backgroundPaint);
      }

      drawingArea.setBackground(new BitmapDrawable(getResources(), background));
    }

    ((TextView)findViewById(R.id.numFrameLabel)).setText(currentPicture + 1 + "/" + layers.size());

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
    invalidCache();
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
    invalidCache();
    setLayer();
  }

  /**
   * Reset the current picture and call setLayer
   * @param view android view
   */
  public void erasePicture(View view) {
    Point size = new Point();
    getWindowManager().getDefaultDisplay().getSize(size);
    Bitmap newLayer = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
    layers.set(currentPicture, newLayer);

    DrawingArea drawingArea = (DrawingArea) findViewById(R.id.drawingAreaView);
    drawingArea.setLayer(newLayer);
  }

  /**
   * Assemble all picture drawn on layers to one video on external storage in directory movies.
   *
   * @param view android view
   */
  public void saveVideo(View view) {
    encodeVideo();
  }

  /**
   * Assemble all picture drawn on layers to one video on external storage in directory movies.
   *
   * @return thread launch to encode video
   */
  public Thread encodeVideo() {
    final ProgressDialog saveProgress = new ProgressDialog(this);
    saveProgress.setTitle("Encoding video in progress");
    saveProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    saveProgress.setMax(layers.size());
    saveProgress.setCancelable(false);
    saveProgress.show();

    Thread encodeVideoThread = new Thread(new Runnable() {
      @Override
      public void run() {
        if (outputVideo == null) {
          String outputVideoName = new SimpleDateFormat("yyyyMMdd_HHmmss",
              Locale.getDefault())
              .format(new Date());

          // create basedir
          outputVideo = new File(Environment.getExternalStoragePublicDirectory(
              Environment.DIRECTORY_MOVIES), String.valueOf(getResources().getText(R.string.app_name)));

          if (outputVideo.isDirectory() || outputVideo.mkdirs()) {
            outputVideo = new File(outputVideo,
                outputVideoName + ".mp4"
            );
          } else {
            return;
          }
        }

        Log.v(TAG, "Start encoding in " + outputVideo.getAbsolutePath());
        try {
          SequenceEncoder encoder = new SequenceEncoder(outputVideo);
          for (Bitmap frame : layers) {

            handler.post(new Runnable() {
              @Override
              public void run() {
                saveProgress.incrementProgressBy(1);
              }
            });

            Picture outputFrame = fromBitmap(frame);
            for (int i = 0; i <= rateFps; ++i) {
              encoder.encodeNativeFrame(outputFrame);
            }
          }
          encoder.finish();

        } catch (IOException e) {
          e.printStackTrace();
        }

        Log.v(TAG, "Encoding finish");
        saveProgress.dismiss();
      }
    });
    encodeVideoThread.start();

    return encodeVideoThread;
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
   * @param view android view
   */
  public void shareVideo(View view) {
    final Thread encodeVideoThread = (outputVideo == null) ? encodeVideo() : new Thread();

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          encodeVideoThread.join();

          File shareFile = new File(new File(getApplicationContext().getCacheDir(), "video"), outputVideo.getName());
          Uri videoUri = FileProvider.getUriForFile(getApplicationContext(), "org.alma.rotoscope.fileprovider", shareFile);
          grantUriPermission("org.alma.rotoscope.fileprovider", videoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

          copy(outputVideo, shareFile);

          Intent shareIntent = new Intent();
          shareIntent.setAction(Intent.ACTION_SEND);
          shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri);
          shareIntent.setType("video/mp4");

          startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));

        } catch (InterruptedException | IOException e) {
          e.printStackTrace();
        }

      }
    }).start();
  }

  /**
   * Use intent Intent.ACTION_VIEW to read the output video
   * @param vew android view
   */
  public void playVideo(View vew) {
    final Thread encodeVideoThread = (outputVideo == null) ? encodeVideo() : new Thread();

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          encodeVideoThread.join();
        } catch (InterruptedException ignore) {}

        Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(outputVideo.getParent()));
        viewIntent.setDataAndType(Uri.parse(outputVideo.getPath()), "video/mp4");
        startActivity(viewIntent);
      }
    }).start();
  }

  /**
   * Open dialog to set nbOnion and set freqOnion
   *
   * @param view android view
   */
  public void onionSkinSettings(View view) {
    final AlertDialog.Builder onionDialogBuilder = new AlertDialog.Builder(this);

    View onionDialogView = getLayoutInflater().inflate(R.layout.dialog_onion_settings, null);
    onionDialogBuilder.setView(onionDialogView);

    ((EditText) onionDialogView.findViewById(R.id.nbOnionInput)).setText(String.valueOf(nbOnion));

    ((EditText) onionDialogView.findViewById(R.id.freqOnionInput)).setText(String.valueOf(freqOnion));

    onionDialogBuilder
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            AlertDialog d = (AlertDialog) dialog;
            try {
              nbOnion = Math.max(Integer.parseInt(((EditText) d.findViewById(R.id.nbOnionInput)).getText().toString()), 0);
            } catch (NumberFormatException e) {
              nbOnion = 0;
            }

            try {
              freqOnion = Math.max(Integer.parseInt(((EditText) d.findViewById(R.id.freqOnionInput)).getText().toString()), 1);
            } catch (NumberFormatException e) {
              freqOnion = 1;
            }

            setLayer();
          }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        });

    onionDialogBuilder.setTitle(R.string.title_dialogOnionSkin);

    onionDialogBuilder.create().show();
  }

  /**
   * Copy the content of src into dest file
   *
   * @param src source file
   * @param dst destination file
   * @throws IOException
   */
  private void copy(File src, File dst) throws IOException {
    File dstParent = dst.getParentFile();
    if (!dstParent.exists()) {
      dstParent.mkdirs();
    }

    InputStream in = new FileInputStream(src);
    OutputStream out = new FileOutputStream(dst);

    // Transfer bytes from in to out
    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
    in.close();
    out.close();
  }

  /**
   * Show the color picker of the paint
   * @param view android view
   */
  public void pickColor(View view) {
    colorPicker.show();
  }

  /**
   * Toggle background between currentFrame and no image
   * @param view android view
   */
  public void toggleBackground(View view) {
    BitmapDrawable background;
    showBackground = !showBackground;

    if (showBackground) {
      background = new BitmapDrawable(getResources(), currentFrame);
      view.setAlpha(0.5f);
    } else {
      Point size = new Point();
      getWindowManager().getDefaultDisplay().getSize(size);

      background = new BitmapDrawable(getResources(),
          Bitmap.createBitmap(size.x, size.y, Bitmap.Config.RGB_565)
      );
      view.setAlpha(1.0f);
    }

    findViewById(R.id.drawingAreaView).setBackground(background);
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

      Log.v(TAG, "Menu visible");

      runHideMenu = new Runnable() {
        @Override
        public void run() {
          fade(menu, false);
          fade(nav, false);
          Log.v(TAG, "Menu invisible");
        }
      };
    }
  }

  /**
   * Animate Fade in or Fade out view
   * @param v view will animate
   * @param fadeIn true if become visible, false become invisible
   */
  private void fade(final View v, boolean fadeIn) {
    if (fadeIn) {
      fade(v, 200, true);

    } else {
      fade(v, 800, false);
    }
  }

  /**
   * Animate Fade in or Fade out view
   * @param v view will animate
   * @param duration duration of animate
   * @param fadeIn true if become visible, false become invisible
   */
  private void fade(final View v, long duration, boolean fadeIn) {

    if (fadeIn && v.getVisibility() != View.VISIBLE) {
      v.setAlpha(0f);
      v.setVisibility(View.VISIBLE);

      v.animate()
          .alpha(1f)
          .setDuration(duration)
          .setListener(null);
    } else if (!fadeIn) {
      v.animate()
          .alpha(0f)
          .setDuration(duration)
          .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
              v.setVisibility(View.GONE);
            }
          });
    }
  }

  /**
   * Get a Frame to input video at a specific picture
   * @param at index of picture
   * @return the Bitmap correspond to the good frame resize to screen size
   */
  private Bitmap getFrameVideo(int at) {
    // calculate the good time for a specific frame correspond currentPicture into video
    final Point screen = new Point();
    getWindowManager().getDefaultDisplay().getSize(screen);

    long time = (long) (rateFps * (at * 1000000 / inputFps));

    Bitmap frame = metadata.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST);
    if (frame == null) {
      frame = metadata.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
    }

    try {
      return Bitmap.createScaledBitmap(frame, screen.x, screen.y, false);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Invalid cache to run invalidCacheRunnable if no thread is already run
   */
  private synchronized void invalidCache() {
    Log.v(TAG, "invalid cache");
    cacheUpToDate = false;

    if (!invalidCacheThread.isAlive()) {
      invalidCacheThread = new Thread(invalidCacheRunnable);
      invalidCacheThread.start();
    }
  }

}
