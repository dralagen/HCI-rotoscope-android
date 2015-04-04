package org.alma.rotoscope;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/**
 * The Main Activity is launch at startup.
 * This activity create new project from new video or from a video on InternalStorage
 *
 * @author dralagen
 */
public class MainActivity extends Activity {

  private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
  private static final int FILE_SELECT_CODE = 100;

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
    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

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
    if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE || requestCode == FILE_SELECT_CODE) {
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
