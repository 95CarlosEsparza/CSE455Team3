package com.example.caballero.cse455_fall17;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.media.ExifInterface;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    final static String TAG = "TEST";
    //for popdialog picker
    ArrayList<Integer> mUserItems = new ArrayList<>();
    //for words to find
    List<String> list = new ArrayList<>();
    //important text
    List<String> importantText = new ArrayList<>();
    //popdialog string array
    String[] listItems;
    //popdialog checkbox array
    boolean[] checkedItems;

    public static final int IMAGE_GALLERY_REQUEST = 2;
    public static final int CAMERA_REQUEST = 1;
    final StringBuilder stringBuilder = new StringBuilder();

    ImageView imgPicture;
    TextView OCRTextView;

    Bitmap image;
    String mCurrentPhotoPath;
    TextManager txt;

    //for the drawer added by Carlos
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.nav_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:               //from              to
                return true;
            case R.id.settings:
                startActivity(new Intent(MainActivity.this, Settings.class));
                this.finish();
                return true;
            case R.id.thememenu:
                startActivity(new Intent(MainActivity.this, Theme.class));
                this.finish();
                return true;
            case R.id.faq:
                startActivity(new Intent(MainActivity.this, FAQ.class));
                this.finish();
                return true;
            case R.id.log:
                startActivity(new Intent(MainActivity.this, com.example.caballero.cse455_fall17.Log.class));
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences theme = getSharedPreferences("THEME_SELECT" , Context.MODE_PRIVATE);
        int themeInt = theme.getInt("THEME", 0);
        if(themeInt != R.layout.activity_main_dark && themeInt != R.layout.activity_main)
            themeInt = R.layout.activity_main;
        if(themeInt == R.layout.activity_main_dark){
            setTheme(R.style.AppThemeDark);
        }
        super.onCreate(savedInstanceState);
        setContentView(themeInt);

        //request permissions
        requestPermissions();

        OCRTextView = (TextView) findViewById(R.id.ocrtext);
        imgPicture = (ImageView) findViewById(R.id.imageView);
        FloatingActionButton gallery = (FloatingActionButton) findViewById(R.id.floatingGallery);
        FloatingActionButton camera = (FloatingActionButton) findViewById(R.id.floatingCamera);
        FloatingActionButton calendar = (FloatingActionButton) findViewById(R.id.floatingCalendar);

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callGallery();
            }
        });
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCamera();
            }
        });
        calendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("content://com.android.calendar/time/")));
            }
        });

    }


    //for professor's page
    public void goToProfessors(View view){
        Intent intent = new Intent(this, ProfessorActivity.class);
        startActivity(intent);
    }

    //open gallery and wait for result
    public void callGallery() {
        stringBuilder.delete(0,stringBuilder.length());
        importantText.clear();
        mUserItems.clear();
        // invoke the image gallery using an implict intent.
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);

        // where do we want to find the data?
        File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String pictureDirectoryPath = pictureDirectory.getPath();
        // finally, get a URI representation
        Uri data = Uri.parse(pictureDirectoryPath);

        // set the data and type.  Get all image types.
        photoPickerIntent.setDataAndType(data, "image/*");

        // we will invoke this activity, and get something back from it.
        startActivityForResult(photoPickerIntent, IMAGE_GALLERY_REQUEST);
    }

    //open camera and wait
    void startCamera() {
        stringBuilder.delete(0,stringBuilder.length());
        importantText.clear();
        mUserItems.clear();
        try {
            dispatchTakePictureIntent();
        } catch (IOException e) {
        }
    }

    //take picture save to path and wait for result
    private void dispatchTakePictureIntent() throws IOException {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        createImageFile());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    //get results, process and display them
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        InputStream inputStream = null;
        if(resultCode != RESULT_CANCELED){
            if (resultCode == RESULT_OK) {
                // if we are here, everything processed successfully.
                if (requestCode == IMAGE_GALLERY_REQUEST) {
                    // if we are here, we are hearing back from the image gallery.

                    // the address of the image on the SD Card.
                    Uri imageUri = data.getData();
                    File imageFile = new File(getRealPathFromURI(imageUri));
                    String path = imageFile.getAbsolutePath();

                    // we are getting an input stream, based on the URI of the image.
                    try {
                        inputStream = getContentResolver().openInputStream(imageUri);

                        // get a bitmap from the stream.
                        image = BitmapFactory.decodeStream(inputStream);


                        rotateImage(imageUri);
                        processImage();

                        // show the image to the user
                        imgPicture.setImageBitmap(image);


                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        // show a message to the user indictating that the image is unavailable.
                        Toast.makeText(this, "Unable to open image", Toast.LENGTH_LONG).show();
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException ignored) {
                            }
                        }

                    }
                }
                if (requestCode == CAMERA_REQUEST) {
                    // Show the thumbnail on ImageView
                    Uri imageUri = Uri.parse(mCurrentPhotoPath);
                    File file = new File(imageUri.getPath());
                    try {
                        InputStream ims = new FileInputStream(file);
                        image = BitmapFactory.decodeStream(ims);

                        rotateImage(imageUri);
                        processImage();


                        imgPicture.setImageBitmap(image);
                    } catch (FileNotFoundException e) {
                        return;
                    }

                    // ScanFile so it will be appeared on Gallery
                    MediaScannerConnection.scanFile(MainActivity.this,
                            new String[]{imageUri.getPath()}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                }
                            });
                }
            }
        }
    }

    //get image path
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    //create path for picture taken
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    //process image to text
    public void processImage(){

        if(image != null){
            TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
            if(!textRecognizer.isOperational()) {
                //Log.w(TAG, "Detector dependencies are not yet available.");
            }
            Frame imageFrame = new Frame.Builder()
                    .setBitmap(image)
                    .build();
            final SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

            if(textBlocks.size() != 0){
                OCRTextView.post(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < textBlocks.size(); ++i){
                            TextBlock item = textBlocks.valueAt(i);
                            stringBuilder.append(item.getValue());
                            stringBuilder.append("\n");
                        }
                        //String exam = txt.stringList(stringBuilder.toString(),"Exam");
                        //OCRTextView.setText(stringBuilder.toString());
                        afterResult();
                    }
                });
            }
        }

    }

    //rotate image if not 0 degrees
    public void rotateImage(Uri tempuri){

        InputStream in = null;
        ExifInterface exifInterface = null;
        try {
            in = getContentResolver().openInputStream(tempuri);
            exifInterface = new ExifInterface(in);
            // Now you can extract any Exif tag you want
            // Assuming the image is a JPEG or supported raw format
        } catch (IOException e) {
            // Handle any errors
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }

        int rotate = 0;
        int orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
        }

        //Log.v(TAG, "Rotation: " + rotate);

        if (rotate != 0) {

            // Getting width & height of the given image.
            int w = image.getWidth();
            int h = image.getHeight();

            // Setting pre rotate
            Matrix mtx = new Matrix();
            mtx.preRotate(rotate);

            // Rotating Bitmap
            image = Bitmap.createBitmap(image, 0, 0, w, h, mtx, false);
        }

        // Convert to ARGB_8888, required by tess
        image = image.copy(Bitmap.Config.ARGB_8888, true);
        //imgPicture.setImageBitmap(image);

    }

    public void afterResult(){
        list = txt.populateList(list, "");
        importantText = txt.importantList(stringBuilder.toString(),list);
        Set<String> tempSet = new HashSet<>(importantText);
        listItems = tempSet.toArray(new String[tempSet.size()]);
        checkedItems = new boolean[listItems.length];
        importantText.clear();
        importantText.addAll(tempSet);


        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        if(listItems != null && listItems.length > 0){
            mBuilder.setTitle("select your sentence");
            mBuilder.setMultiChoiceItems(listItems, checkedItems, new DialogInterface.OnMultiChoiceClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int position, boolean isChecked){
                    if(isChecked){
                        mUserItems.add(position);
                    }else{
                        mUserItems.remove((Integer.valueOf(position)));
                    }
                }
            });
            mBuilder.setCancelable(false);
            mBuilder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String item = "";
                    for(int i = 0; i< mUserItems.size(); i++){
                        item = item + listItems[mUserItems.get(i)];
                        if(txt.isDate(listItems[mUserItems.get(i)])){
                            addToCalendar(listItems[mUserItems.get(i)]);
                        }
                        if(txt.isContact(listItems[mUserItems.get(i)])){
                            SharedPreferences prof = getSharedPreferences("PROFESSOR_SELECT" , Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prof.edit();
                            editor.putString("PROFESSOR", listItems[mUserItems.get(i)]);
                            Log.v(TAG,"11 "+listItems[mUserItems.get(i)] );
                            editor.apply();

                            startActivityForResult(txt.contactDetect(listItems[mUserItems.get(i)]), 1);
                        }
                    }
                    OCRTextView.setText(item);
                }
            });
            mBuilder.setNegativeButton("cancel", new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            mBuilder.setNeutralButton("select all",new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, final int which){
                    for (int i = 0; i < checkedItems.length; i++) {
                        checkedItems[i] = false;
                        mUserItems.clear();
                        //mItemSelected.setText("");
                    }
                    String item = "";
                    for(int j = 0; j< importantText.size(); j++){
                        item = item + importantText.get(j);
                        if(txt.isDate(importantText.get(j))){
                            addToCalendar(importantText.get(j));
                        }
                        if(txt.isContact(importantText.get(j))){

                            SharedPreferences prof = getSharedPreferences("PROFESSOR_SELECT" , Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prof.edit();
                            editor.putString("PROFESSOR", importantText.get(j));
                            editor.apply();

                            startActivityForResult(txt.contactDetect(importantText.get(j)), 1);
                        }
                    }
                    OCRTextView.setText(item);
                }
            });
        } else {
            mBuilder.setTitle("no Important text");
            mBuilder.setCancelable(false);
            mBuilder.setNegativeButton("cancel", new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            OCRTextView.setText("no important text");
        }
        AlertDialog mDialog = mBuilder.create();
        mDialog.show();
    }

    //permissions
    public void requestPermissions(){
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_CALENDAR};
        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }
    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
    //automatically adds dates to calendar
    public void addToCalendar(String s){

        String day = txt.dateDetect(s);
        SimpleDateFormat yyyyMMdd = new SimpleDateFormat("MMddyyyyHH");/*android.icu.text.*/
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(yyyyMMdd.parse(day));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        cal.add(Calendar.DATE, 0);

        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();

        values.put(CalendarContract.Events.DTSTART, cal.getTimeInMillis());
        values.put(CalendarContract.Events.TITLE, "Smart Syllabus Event");
        values.put(CalendarContract.Events.DESCRIPTION, s);

        TimeZone timeZone = TimeZone.getDefault();
        values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());

        // Default calendar
        values.put(CalendarContract.Events.CALENDAR_ID, 3);

        // Set Period for 1 Hour
        values.put(CalendarContract.Events.DURATION, "+P1H");

        values.put(CalendarContract.Events.HAS_ALARM, 1);

        // Insert event to calendar
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
    }
}
