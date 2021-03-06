package com.example.plus_button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //private static final int MY_PERMISSION_STORAGE = 1111;
    private static final int MY_PERMISSION_CAMERA = 2222;
    private static final int REQUEST_TAKE_PHOTO = 3333;
    private static final int REQUEST_TAKE_ALBUM = 4444;
    private static final int REQUEST_IMAGE_CROP = 5555;
    private static final int REQUEST_UPLOAD = 6666;
    private static final String TAG = "GPS";

    private Animation fab_open, fab_close;
    private Boolean isFabOpen = false;
    private FloatingActionButton plus, camera, album, upload;
    private FirebaseStorage storage;
    private FusedLocationProviderClient fusedLocationClient;

    ImageView iv_view;

    String mCurrentPhotoPath;

    Uri imageUri;
    Uri photoURI, albumURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //???????????? ????????????
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //?????????
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);

        plus = (FloatingActionButton) findViewById(R.id.plus);
        camera = (FloatingActionButton) findViewById(R.id.camera);
        album = (FloatingActionButton) findViewById(R.id.album);
        upload = (FloatingActionButton) findViewById(R.id.upload);
        iv_view = (ImageView) findViewById(R.id.iv_view);

        storage = FirebaseStorage.getInstance();

        plus.setOnClickListener(this);
        camera.setOnClickListener(this);
        album.setOnClickListener(this);
        upload.setOnClickListener(this);

        checkPermission();
    }

//    final TextView textview_address = (TextView)findViewById(R.id.textview);

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // ?????? ???????????? if()?????? ????????? false??? ?????? ??? -> else{..}??? ???????????? ?????????
            if ((ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) ||
                    (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))) {
                new AlertDialog.Builder(this)
                        .setTitle("??????")
                        .setMessage("????????? ????????? ?????????????????????. ????????? ???????????? ???????????? ?????? ????????? ?????? ??????????????? ?????????.")
                        .setNeutralButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        })
                        .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, MY_PERMISSION_CAMERA);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_CAMERA:
                for (int i = 0; i < grantResults.length; i++) {
                    // grantResults[] : ????????? ????????? 0, ????????? ????????? -1
                    if (grantResults[i] < 0) {
                        Toast.makeText(MainActivity.this, "?????? ????????? ????????? ????????? ?????????.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                // ??????????????? ??? ????????????..

                break;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.plus:
                anim();
                //Toast.makeText(this, "plus", Toast.LENGTH_SHORT).show();
                break;
            case R.id.camera:
                anim();
                //Toast.makeText(this, "camera", Toast.LENGTH_SHORT).show();
                captureCamera();
                break;
            case R.id.album:
                anim();
                //Toast.makeText(this, "album", Toast.LENGTH_SHORT).show();
                getAlbum();
                break;
            case R.id.upload:
                anim();
                //Toast.makeText(this, "upload", Toast.LENGTH_SHORT).show();
                loadAlbum();
                break;
        }
    }

    //?????? ??????
    private void captureCamera() {
        String state = Environment.getExternalStorageState();
        // ?????? ????????? ??????
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();

                } catch (IOException ex) {
                    Log.e("captureCamera Error", ex.toString());
                }
                if (photoFile != null) {
                    // getUriForFile??? ??? ?????? ????????? Manifest provider??? authorites??? ???????????? ???

                    Uri providerURI = FileProvider.getUriForFile(this, getPackageName(), photoFile);
                    imageUri = providerURI;

                    // ???????????? ????????? ?????? FileProvier??? Return?????? content://??????!!, providerURI??? ?????? ????????? ???????????? ?????? ??????
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerURI);

                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }
            }
        } else {
            Toast.makeText(this, "??????????????? ?????? ???????????? ???????????????", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";
        File imageFile = null;
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures", "youn");
        //permission check + ?????? ?????? ?????? ?????????
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location.
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                        //set exif
                            String filename = Environment.getExternalStorageDirectory().getPath() + "/Pictures/youn/" + imageFileName;
                            SetLocaExif(filename, latitude, longitude);
                            TextView text = (TextView)findViewById(R.id.loca_data);
                            //text.append(Double.toString(latitude)+"\n"+Double.toString(longitude));
                        }
                        else{
                            Toast.makeText(getApplicationContext(), "Error!", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        //?????????
        if (!storageDir.exists()) {
            Log.i("mCurrentPhotoPath1", storageDir.toString());
            storageDir.mkdirs();
        }

        imageFile = new File(storageDir, imageFileName);
        mCurrentPhotoPath = imageFile.getAbsolutePath();

        return imageFile;
    }

    //CROP ???
    private void getAlbum(){
        Log.i("getAlbum", "Call");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, REQUEST_TAKE_ALBUM);
    }

    //UPLOAD ???
    private void loadAlbum(){
        Log.i("loadAlbum", "Call");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, REQUEST_UPLOAD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Log.i("REQUEST_TAKE_PHOTO", "OK");
                        galleryAddPic();

                        iv_view.setImageURI(imageUri);
                    } catch (Exception e) {
                        Log.e("REQUEST_TAKE_PHOTO", e.toString());
                    }
                } else {
                    Toast.makeText(MainActivity.this, "????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_TAKE_ALBUM:
                if (resultCode == Activity.RESULT_OK) {

                    if (data.getData() != null) {
                        try {
                            File albumFile = null;
                            albumFile = createImageFile();
                            photoURI = data.getData();
                            albumURI = Uri.fromFile(albumFile);
                            cropImage();
                        } catch (Exception e) {
                            Log.e("TAKE_ALBUM_SINGLE ERROR", e.toString());
                        }
                    }
                }
                break;

            case REQUEST_IMAGE_CROP:
                if (resultCode == Activity.RESULT_OK) {

                    galleryAddPic();
                    iv_view.setImageURI(albumURI);
                }
                break;

            case REQUEST_UPLOAD:
                if (resultCode == Activity.RESULT_OK) {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String imageFileName = "JPEG_" + timeStamp + ".jpg";

                    Uri file = data.getData();
                    StorageReference storageRef = storage.getReference();
                    StorageReference riversRef = storageRef.child("image/"+imageFileName);
                    UploadTask uploadTask = riversRef.putFile(file);

                    try {
                        InputStream in = getContentResolver().openInputStream(data.getData());
                        Bitmap img = BitmapFactory.decodeStream(in);
                        in.close();
                        iv_view.setImageBitmap(img);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "?????? ???????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(MainActivity.this, "?????? ???????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    //?????? ?????? ????????? ???????????? Environment.getExternalStorageDirectory().getPath()
                    String filename = Environment.getExternalStorageDirectory().getPath() + "/Pictures/youn/" + imageFileName;
                    try {
                        ExifInterface exif = new ExifInterface(filename);
                        showExif(exif);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    private void galleryAddPic(){
        Log.i("galleryAddPic", "Call");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        // ?????? ????????? ?????? ????????? ?????????(?????? ????????? ???????????? ????????? ???????????? ??? ???)
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
        Toast.makeText(this, "????????? ????????? ?????????????????????.", Toast.LENGTH_SHORT).show();
    }

    // ????????? ?????? ??????
    public void cropImage(){
        Log.i("cropImage", "Call");
        Log.i("cropImage", "photoURI : " + photoURI + " / albumURI : " + albumURI);

        Intent cropIntent = new Intent("com.android.camera.action.CROP");

        // 50x50??????????????? ????????? ??? ????????? ?????? ?????? + ?????????, ?????? ?????? ???????????? ??????
        cropIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cropIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cropIntent.setDataAndType(photoURI, "image/*");
        //cropIntent.putExtra("outputX", 200); // crop??? ???????????? x??? ??????, ???????????? ??????
        //cropIntent.putExtra("outputY", 200); // crop??? ???????????? y??? ??????
        cropIntent.putExtra("aspectX", 1); // crop ????????? x??? ??????, 1&1?????? ????????????
        cropIntent.putExtra("aspectY", 1); // crop ????????? y??? ??????
        cropIntent.putExtra("scale", true);
        cropIntent.putExtra("output", albumURI); // ????????? ???????????? ?????? ????????? ??????
        startActivityForResult(cropIntent, REQUEST_IMAGE_CROP);
    }

    private void anim() {
        if (isFabOpen) {
            camera.startAnimation(fab_close);
            camera.setClickable(false);
            album.startAnimation(fab_close);
            album.setClickable(false);
            upload.startAnimation(fab_close);
            upload.setClickable(false);
            isFabOpen = false;
        } else {
            camera.startAnimation(fab_open);
            camera.setClickable(true);
            album.startAnimation(fab_open);
            album.setClickable(true);
            upload.startAnimation(fab_open);
            upload.setClickable(true);
            isFabOpen = true;
        }
    }
    public void SetLocaExif(String image_location, Double latitude, Double longitude){
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(image_location);
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPS.convert(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPS.convert(longitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GPS.latitudeRef(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GPS.longitudeRef(longitude));
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };
    private void showExif(ExifInterface exif) {
        TextView text = (TextView)findViewById(R.id.loca_data);
        String myAttribute = "[Exif information] \n\n";
        myAttribute += getTagString(ExifInterface.TAG_GPS_LATITUDE, exif);
        myAttribute += getTagString(ExifInterface.TAG_GPS_LATITUDE_REF, exif);
        myAttribute += getTagString(ExifInterface.TAG_GPS_LONGITUDE, exif);
        myAttribute += getTagString(ExifInterface.TAG_GPS_LONGITUDE_REF, exif);
        text.append(myAttribute);
    }
    public void findExif(String imageFileName){
        String filename = Environment.getExternalStorageDirectory().getPath() + "/Pictures/youn/" + imageFileName;
        try {
            ExifInterface exif = new ExifInterface(filename);
            showExif(exif);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
        }
    }
    private String getTagString(String tag, ExifInterface exif){
        return (tag+":"+exif.getAttribute(tag)+"\n");
    }
    public static class GPS {
        private static StringBuilder sb = new StringBuilder(20);

        public static String latitudeRef(double latitude) {
            return latitude<0.0d?"S":"N";
        }
        public static String longitudeRef(double longitude) {
            return longitude<0.0d?"W":"E";
        }
        synchronized public static final String convert(double latitude) {
            latitude=Math.abs(latitude);
            int degree = (int) latitude;
            latitude *= 60;
            latitude -= (degree * 60.0d);
            int minute = (int) latitude;
            latitude *= 60;
            latitude -= (minute * 60.0d);
            int second = (int) (latitude*1000.0d);

            sb.setLength(0);
            sb.append(degree);
            sb.append("/1,");
            sb.append(minute);
            sb.append("/1,");
            sb.append(second);
            sb.append("/1000");
            return sb.toString();
        }
    }
}