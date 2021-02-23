package com.test.eyedis;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraX;
import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback{

    private Dialog mDialog, info_Dialog;
    private int REQUEST_CODE_PERMISSIONS = 101;
    private String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    private TextureView textureView;
    private ImageView imageView;
    private boolean flash, focus, gpu, voice;
    private ImageClassifier classifer;
    private TextToSpeech mTTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDialog = new Dialog(this);
        mDialog.setCanceledOnTouchOutside(false);
        info_Dialog = new Dialog(this);
        info_Dialog.setCanceledOnTouchOutside(false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        flash = getIntent().getBooleanExtra("flash", true);
        focus = getIntent().getBooleanExtra("focus", true);
        gpu = getIntent().getBooleanExtra("gpu", false);
        voice = getIntent().getBooleanExtra("voice", true);

        classifer = new ImageClassifier(this);

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.ENGLISH);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    }
                } else {
                    Log.e("TTS", "Initialization failed.");
                }
            }
        });

        textureView = (TextureView) findViewById(R.id.view_finder);
        if (allPermissionGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        CameraX.unbindAll();

        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight());

        PreviewConfig pConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetResolution(screen)
                .build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });

        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        if (flash) {
            imgCap.setFlashMode(FlashMode.AUTO);
        } else {
            imgCap.setFlashMode(FlashMode.OFF);
        }

        imageView = findViewById(R.id.imgCapture);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        "picture.jpg");
                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        Bitmap bMap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        Bitmap newMap = croppedBitmap(bMap);
                        //saveImage(newMap);
                        classifyItem(newMap);
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError,
                                        @NonNull String message, @Nullable Throwable cause) {

                        String msg = "Pic captured failed: " + message;
                        Toast.makeText(getBaseContext(),msg,Toast.LENGTH_LONG).show();

                        if (cause != null) {
                            cause.printStackTrace();
                        }
                    }
                });
            }
        });

        CameraX.bindToLifecycle(this, preview, imgCap);
    }

    private void classifyItem(Bitmap bitmap) {
        if (classifer == null) {
            Toast.makeText(getBaseContext(), "Null Classifier", Toast.LENGTH_LONG).show();
        }

        String result = classifer.classifyFrame(bitmap, gpu);
        bitmap.recycle();

        if (result.equals("")) {
            result = "Failed to classify";
        }

        if (voice) {
            voice_out(result);
        }
        Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        classifer.close();

        voice_out("");
        mTTS.stop();
        mTTS.shutdown();
    }

    private Bitmap croppedBitmap(Bitmap bMap) {
        Bitmap sendMap;

        Matrix matrix = new Matrix();
        matrix.postRotate(90);


        if (bMap.getWidth() >= bMap.getHeight()){

            sendMap = Bitmap.createBitmap(
                    bMap,
                    bMap.getWidth()/2 - bMap.getHeight()/2,
                    0,
                    bMap.getHeight(),
                    bMap.getHeight(),
                    matrix,
                    true
            );

        }else{

            sendMap = Bitmap.createBitmap(
                    bMap,
                    0,
                    bMap.getHeight()/2 - bMap.getWidth()/2,
                    bMap.getWidth(),
                    bMap.getWidth(),
                    matrix,
                    true
            );
        }


        return sendMap;
    }

    private void updateTransform(){

        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)textureView.getRotation();

        switch(rotation){
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float)rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    private boolean allPermissionGranted() {

        for(String permission : REQUIRED_PERMISSIONS){

            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){

                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setting:
                Intent i = new Intent(getApplicationContext(), SettingActivity.class);
                i.putExtra("flash",flash);
                i.putExtra("focus",focus);
                i.putExtra("gpu",gpu);
                i.putExtra("voice",voice);
                startActivity(i);
                return true;

            case R.id.guide:
                if (mDialog != null) {
                    mDialog.setContentView(R.layout.guideline);
                    mDialog.getWindow()
                            .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    mDialog.show();
                    if (voice) {
                        voice_out();
                    }
                }
                return true;

                default:
                    return super.onOptionsItemSelected(item);
        }
    }

    public void showInfoDialog(View v) {
        if (info_Dialog != null) {
            info_Dialog.setContentView(R.layout.app_info);
            info_Dialog.getWindow()
                    .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            info_Dialog.show();
        }
    }

    public void closeDialog(View v){
        if (voice) {
            voice_out("");
        }
        mDialog.dismiss();
    }

    public void closeInfoDialog(View v) {
        if (voice) {
            voice_out("");
        }
        info_Dialog.dismiss();
    }

    public void voice_out() {
        String text_1 = getResources().getString(R.string.text_1);
        String text_2 = getResources().getString(R.string.text_2);

        mTTS.setSpeechRate(0.80f);

        mTTS.speak(text_1, TextToSpeech.QUEUE_FLUSH, null);
        mTTS.speak(text_2, TextToSpeech.QUEUE_ADD, null);
    }

    public void voice_out(String text) {
        mTTS.setSpeechRate(0.80f);

        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
}
