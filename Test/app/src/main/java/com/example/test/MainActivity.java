package com.example.test;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity{
    private ClipVideoView clip_view;
    
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        clip_view = findViewById(R.id.clip_view);
        clip_view.setIClipVideoListener(new ClipVideoView.IClipVideoListener(){
            @Override
            public void onFinishVideo(boolean isSuccess){
                Toast.makeText(MainActivity.this, "" + isSuccess, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onClipFinshVideo(String path){
                Toast.makeText(MainActivity.this, "" + path, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onCancel(){
                finish();
            }
        });
    }
    
    
    private void startTo(){
        Intent mediaChooser = new Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        mediaChooser.setType("image/*,video/*");
        startActivityForResult(mediaChooser, 1);
    }
    
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK) return;
        switch(requestCode){
            case 1://相册
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                assert selectedImage != null;
                Cursor cursor = this.getContentResolver().query(selectedImage, filePathColumn,
                    null, null, null);
                if(cursor != null && cursor.moveToFirst()){
                    String picturePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
                    File file = new File(picturePath);
                    if(!TextUtils.isEmpty(picturePath) && picturePath.endsWith(".mp4")){
                        cursor.close();
                        clip_view.setPath(file.getAbsolutePath(),
                            getExternalCacheDir().getAbsolutePath() + "/clipvideo");
                    }
                }
                break;
        }
    }
    
    public void onStartTo(View view){
        startTo();
    }
    
    
    @Override
    protected void onDestroy(){
        if(clip_view != null){
            clip_view.onDestroy();
        }
        super.onDestroy();
    }
    
    @Override
    protected void onPause(){
        super.onPause();
        if(clip_view != null){
            clip_view.onPause();
        }
    }
    
    
    @Override
    protected void onResume(){
        super.onResume();
        if(clip_view != null){
            clip_view.onResume();
        }
    }
}