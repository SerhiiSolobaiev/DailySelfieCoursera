package com.coursera.example.dailyselfiecoursera;

import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends ListActivity {
    private static final int ACTION_TAKE_PHOTO = 1;
    private static String TAG = "TagMainActivity";

    private static SelfieListAdapter mImagesAdapter;
    private Uri mCurrentImageUri = null;
    private String mThumbnailFolder = null;
    private String mImageFolder = null;
    private final String mSaveFilename = "SelfieList.json";

    private static final long INITIAL_ALARM_DELAY = 2 * 60 * 1000L; //2 minutes
    private static final long REPEAT_ALARM_DELAY = 2 * 60 * 1000L;

    private PendingIntent mNotificationReceiverPendingIntent;
    private Intent mNotificationReceiverIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        initFolderNames();

        List selfies = new ArrayList();

        mImagesAdapter = new SelfieListAdapter(MainActivity.this,selfies);
        setListAdapter(mImagesAdapter);
        Log.v(TAG, "in onCreate " + mImagesAdapter.toString());

        ListView v = getListView();
        v.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                Selfie s = mImagesAdapter.getItem(position);
                showBigPicture(s.getFullimageUri());
            }
        });

        setupAlarm();
    }

    @Override
    protected void onResume() {
        super.onResume();

        List<Selfie> selfies = loadSelfies();

        if (selfies.isEmpty())
            return;

        mImagesAdapter.clear();
        for (Selfie s : selfies)
            mImagesAdapter.add(s);
    }

    private void initFolderNames() {
        mThumbnailFolder = Environment.DIRECTORY_PICTURES + "/thumbnails";
        mImageFolder = Environment.DIRECTORY_PICTURES + "/images";
        File folder = MainActivity.this.getExternalFilesDir(mThumbnailFolder);
        if (!folder.exists())
            folder.mkdirs();
        Log.i(TAG, "mThumbnailFolder:" + folder.toString());

        folder = MainActivity.this.getExternalFilesDir(mImageFolder);
        if (!folder.exists())
            folder.mkdirs();
        Log.i(TAG, "mImageFolder:" + folder.toString());

    }
    private void showBigPicture(Uri imageUri) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(imageUri, "image/jpg");
        startActivity(intent);
    }

    private void setupAlarm() {
        mNotificationReceiverIntent = new Intent(MainActivity.this, NotificationReceiver.class);

        mNotificationReceiverPendingIntent = PendingIntent.getBroadcast(this, 0,
                mNotificationReceiverIntent, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INITIAL_ALARM_DELAY, REPEAT_ALARM_DELAY,
                mNotificationReceiverPendingIntent);
        Log.v(TAG, "in setupAlarm()");
    }

    private void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(mNotificationReceiverPendingIntent);
            Toast.makeText(getApplicationContext(), "Alarm canceled", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchCamera() {
        // launch camera app
        dispatchTakePictureIntent(ACTION_TAKE_PHOTO);
    }
    private void dispatchTakePictureIntent(int actionCode) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File f = createImageFile(false);
        mCurrentImageUri = Uri.fromFile(f);

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentImageUri);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, actionCode);
        }
        else
            Toast.makeText(this,"Could not launch the camera.",Toast.LENGTH_LONG).show();
    }
    private File createImageFile(boolean thumbnail)  {
        // Create a media file name
        String timeStamp = new SimpleDateFormat("dd_MM_yyyy_HH_mm").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";

        File storageDir = thumbnail?
                MainActivity.this.getExternalFilesDir(mThumbnailFolder):
                MainActivity.this.getExternalFilesDir(mImageFolder);
        String suffix = thumbnail?".png":".jpg";

        try {
            return File.createTempFile(
                    imageFileName,  /* prefix */
                    suffix,          /* suffix */
                    storageDir      /* directory */
            );
        }catch (IOException e) {
            Log.d(TAG, "Error creating image file: " + e.getMessage());
            return null;
        }
    }

    private List loadSelfies() {

        List selfies = new ArrayList();

        Log.v(TAG,"in loadSelfies "+MainActivity.this.getExternalFilesDir(null)+"= "+mSaveFilename);
        File f = new File(MainActivity.this.getExternalFilesDir(null),mSaveFilename);
        if (!f.exists())
            return selfies;

        StringBuilder text = new StringBuilder();
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(f));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            // do exception handling
            Log.v(TAG, "Exception in loadSelfies()");
        } finally {
            try { br.close(); } catch (Exception e) { }
        }

        try {
            JSONArray array = new JSONArray(text.toString());
            for (int i = 0;i<array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                selfies.add(new Selfie(obj));
            }

        } catch (JSONException e) {
            Log.i(TAG, "loadSelfies() JSON Exception: " + e.getMessage());
        }

        return selfies;
    }


    private void saveImageAsSelfie(Uri fullimageUri) {
        String timeStamp = new SimpleDateFormat("dd_MM_yyyy_HH_mm").format(new Date());
        Selfie selfie = new Selfie(timeStamp);
        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.
                decodeFile(fullimageUri.getPath()), 64, 64);
        Uri thumbnailUri = storeImage(thumbImage);
        selfie.setThumbnailUri(thumbnailUri);
        selfie.setFullimageUri(fullimageUri);
        saveSelfieInJson(selfie);
    }
    private Uri storeImage(Bitmap image) {
        File pictureFile = createImageFile(true);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");
            return null;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
            return Uri.fromFile(pictureFile);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        return null;
    }

    private void saveSelfieInJson(Selfie selfie) {

        List<Selfie> selfies = loadSelfies();
        selfies.add(selfie);

        JSONArray jsonArray = new JSONArray();
        for (Selfie s : selfies) {
            jsonArray.put(s.getJSONObject());
        }

        File f = new File(MainActivity.this.getExternalFilesDir(null),mSaveFilename);
        if (f.exists())
            f.delete();

        try {
            FileWriter out = new FileWriter(f);
            out.write(jsonArray.toString());
            out.flush();
            out.close();

        } catch (IOException e) {
            Log.i(TAG, "Error writing file: " + e.getMessage());
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG,"in onActivityResult()");
        switch (requestCode) {
            case ACTION_TAKE_PHOTO: {
                if (resultCode == RESULT_OK) {
                    saveImageAsSelfie(mCurrentImageUri);
                }
                else
                    Toast.makeText(this,"The pic was not taken",Toast.LENGTH_LONG).show();
            }
            break;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_camera:
                launchCamera();
                return true;
            case R.id.cancel_alarm:
                cancelAlarm();
                return true;
            case R.id.delete_all:
                deleteAll();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void deleteAll() {
        File f = new File(MainActivity.this.getExternalFilesDir(null),mSaveFilename);
        f.delete();

        deleteFolderContents(mThumbnailFolder);
        deleteFolderContents(mImageFolder);
        mImagesAdapter.clear();
        Toast.makeText(getApplicationContext(),"All images deleted", Toast.LENGTH_SHORT).show();
    }

    private void deleteFolderContents(String folder) {
        File dir = MainActivity.this.getExternalFilesDir(folder);
        if (dir.exists())
        {
            String[] files = dir.list();
            for (int i = 0; i < files.length; i++)
                new File(dir, files[i]).delete();
        }
    }
}
