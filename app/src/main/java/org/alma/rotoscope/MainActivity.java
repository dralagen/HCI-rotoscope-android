package org.alma.rotoscope;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity {

  private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
  private static final int FILE_SELECT_CODE = 100;
  private Uri fileUri;

  public static final int MEDIA_TYPE_IMAGE = 1;
  public static final int MEDIA_TYPE_VIDEO = 2;

  /** Create a file Uri for saving an image or video */
  private static Uri getOutputMediaFileUri(){
    File f = getOutputMediaFile();
    if (f != null) {
      return Uri.fromFile(f);
    }
    return null;
  }

  /** Create a File for saving the video */
  private static File getOutputMediaFile(){
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.

    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES), String.valueOf(R.string.app_name));
    // This location works best if you want the created images to be shared
    // between applications and persist after your app has been uninstalled.

    // Create the storage directory if it does not exist
    if (! mediaStorageDir.exists()){
      if (! mediaStorageDir.mkdirs()){
        Log.d(String.valueOf(R.string.app_name), "failed to create directory");
        return null;
      }
    }

    // Create a media file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

    return new File(mediaStorageDir.getPath() + File.separator +
          "VID_"+ timeStamp + ".mp4");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    switch (id) {
      case R.id.action_settings :
        startActivity(new Intent(this, SettingsActivity.class));
        return true;

    }

    return super.onOptionsItemSelected(item);
  }

  public void getFilmByCam(View view) {
    //create new Intent
    Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

    fileUri = getOutputMediaFileUri();  // create a file to save the video
    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);  // set the image file name

    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high

    // start the Video Capture Intent
    startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);

  }

  public void getFilmByFileSystem(View view) {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType("video/*");
    intent.addCategory(Intent.CATEGORY_OPENABLE);

    try {
      startActivityForResult(
          Intent.createChooser(intent, "Select a Movie"),
          FILE_SELECT_CODE);
    } catch (android.content.ActivityNotFoundException ex) {
      // Potentially direct the user to the Market with a Dialog
      Toast.makeText(this, "Please install a File Manager.",
          Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    String resourcePath = null;
    if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
      if (resultCode == RESULT_OK) {
        resourcePath = fileUri.getPath();
      }
    } else if (requestCode == FILE_SELECT_CODE) {
      if (resultCode == RESULT_OK) {
        resourcePath = data.getDataString();
      }
    }

    if (resourcePath != null) {
      Intent drawingArea = new Intent(this, DrawingActivity.class);
      drawingArea.putExtra("resourcePath", resourcePath);
      startActivity(drawingArea);
    }
  }
}
