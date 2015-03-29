package org.alma.rotoscope;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class DrawingActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle bundle = getIntent().getExtras();
    String resourcePath = bundle.getString("resourcePath");

    System.out.println("resource path : " + resourcePath);

    MediaMetadataRetriever metadata = new MediaMetadataRetriever();
    metadata.setDataSource(this, Uri.parse(resourcePath));

    setContentView(R.layout.activity_drawing);

    View drawingArea = findViewById(R.id.drawingAreaView);
    drawingArea.setBackground(new BitmapDrawable(getResources(), metadata.getFrameAtTime(0, MediaMetadataRetriever.OPTION_NEXT_SYNC)));

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
