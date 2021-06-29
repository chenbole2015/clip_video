package com.example.test;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @ProjectName: Test
 * @Package: com.example.test
 * @ClassName: ClipVideoView
 * @Description: java类作用描述
 * @Author: zzj
 * @CreateDate: 2021-06-23 0023 9:49
 * @UpdateUser: 更新者
 * @UpdateDate: 2021-06-23 0023 9:49
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 *
 *
 * 适用本地视频截取
 * 
 */
public class ClipVideoView extends RelativeLayout implements View.OnTouchListener,
    View.OnClickListener{
    private RecyclerView mRecyclerView;
    private View view_index; //当前位置
    private ImageView image_s_d;
    private TextView mTextViewCancel; //取消按钮
    private TextView mTextViewSuccess; //点击保存按钮
    private VideoView view_video; //点击保存按钮
    private volatile int maxPic = 10; //默认放多少张 （1秒一张）
    private volatile int duration, maxDurtion = 10000; //视频毫秒秒数   截取最大毫秒数
    private volatile List<Bitmap> strings = new ArrayList<>(); //存放帧图
    private volatile int with; //屏幕宽度
    private volatile int childwith; //每一张图片宽度
    private volatile int leftIndex = 0;
    private volatile int rightIndex = 10;
    private volatile int picSec = 1000;//一张图相当于1000毫秒秒
    private volatile String path; //视频路径
    private String desPath; //源路径
    
    @Override
    public void onClick(View view){
        if(view.getId() == R.id.text_success){
            onSave();
        }else{
            if(mIClipVideoListener != null){
                mIClipVideoListener.onCancel();
            }
        }
    }
    
    
    public interface IClipVideoListener{
        //部署完成
        void onFinishVideo(boolean isSuccess);
        
        //截取完成
        void onClipFinshVideo(String path);
        
        //取消
        void onCancel();
        
    }
    
    private IClipVideoListener mIClipVideoListener;
    
    public void setIClipVideoListener(IClipVideoListener IClipVideoListener){
        mIClipVideoListener = IClipVideoListener;
    }
    
    public ClipVideoView(Context context){
        super(context);
        initView(context);
    }
    
    public ClipVideoView(Context context, AttributeSet attrs){
        super(context, attrs);
        initView(context);
    }
    
    public ClipVideoView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        initView(context);
    }
    
    //保存截取视频
    private void onSave(){
        if(view_video.isPlaying()){
            view_video.pause();
            view_video.stopPlayback();
        }
        removeCallbacks(mRunnable);
        view_index.removeCallbacks(mRunnableIndex);
        view_video.setOnCompletionListener(null);
        new Thread(){
            @Override
            public void run(){
                super.run();
                boolean isSuccess = false;
                try{
                    VideoClipUtils.clip(path, desPath + "/clip.mp4", leftIndex * picSec,
                        rightIndex * picSec);
                    isSuccess = true;
                }catch(IOException e){
                    isSuccess = false;
                }
                boolean finalIsSuccess = isSuccess;
                ClipVideoView.this.post(new Runnable(){
                    @Override
                    public void run(){
                        if(mIClipVideoListener != null){
                            mIClipVideoListener.onClipFinshVideo(finalIsSuccess ? desPath +
                                "/clip.mp4" : "");
                        }
                    }
                });
            }
        }.start();
        
    }
    
    //设置最大毫秒数
    public void setMaxDurtion(int maxDurtion){
        this.maxDurtion = maxDurtion;
    }
    
    //设置视频路径
    public void setPath(String path, String desPath){
        this.path = path;
        view_video.setVideoPath(path);
        this.desPath = desPath;
        new Thread(){
            @Override
            public void run(){
                super.run();
                //获取帧图片  1获取时长，设置最大获取
                MediaPlayer mediaPlayer = new MediaPlayer();
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                try{
                    //获取毫秒数
                    mediaPlayer.setDataSource(path);
                    mediaPlayer.prepare();
                    duration = mediaPlayer.getDuration();
                    mmr.setDataSource(path);
                    //获取第一帧图像的bitmap对象
                    maxPic = duration / picSec;
                    childwith = with / maxPic;
                    strings.clear();
                    //文件夹
                    File fileDir = new File(desPath);
                    if(fileDir != null && fileDir.getParentFile() != null && !fileDir.getParentFile().exists())
                        fileDir.mkdirs();
                    //获取帧率
                    for(int i = 0 ; i < maxPic ; i++){
                        Bitmap frameAtTime = mmr.getFrameAtTime(i * picSec * 1000);
                        strings.add(frameAtTime);
                    }
                }catch(Throwable ex){
                    ex.printStackTrace();
                }finally{
                    mmr.release();
                }
                
                ClipVideoView.this.post(new Runnable(){
                    @Override
                    public void run(){
                        if(strings.size() > 0){
                            play(0);
                            mTextViewSuccess.setVisibility(VISIBLE);
                            image_s_d.setVisibility(VISIBLE);
                            if(maxDurtion < duration){
                                rightIndex = maxDurtion / picSec;
                                
                                RelativeLayout.LayoutParams layoutParams =
                                    (LayoutParams) image_s_d.getLayoutParams();
                                layoutParams.rightMargin = with - (maxDurtion / picSec * childwith);
                                image_s_d.setLayoutParams(layoutParams);
                                
                            }else{
                                rightIndex = with / childwith;
                            }
                            
                            mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(),
                                strings.size()));
                            mRecyclerView.setAdapter(new RecyclerView.Adapter(){
                                @NonNull
                                @Override
                                public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
                                    return new RecyclerView.ViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.item_pic, null)){};
                                }
                                
                                @Override
                                public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position){
                                    ImageView imageView = holder.itemView.findViewById(R.id.image);
                                    imageView.setImageBitmap(strings.get(position));
                                }
                                
                                @Override
                                public int getItemCount(){
                                    return strings.size();
                                }
                            });
                        }
                        if(mIClipVideoListener != null){
                            mIClipVideoListener.onFinishVideo(strings.size() > 0);
                        }
                    }
                });
                
            }
        }.start();
        
    }
    
    private int currentSec;
    
    private Runnable mRunnable = new Runnable(){
        @Override
        public void run(){
            if(currentSec <= rightIndex){
                currentSec++;
                postDelayed(this, 1000);
            }else{
                play(leftIndex * picSec);
            }
        }
    };
    
    private int curentIndexx;
    private Runnable mRunnableIndex = new Runnable(){
        @Override
        public void run(){
            if(view_index.getVisibility() == INVISIBLE) view_index.setVisibility(VISIBLE);
            if(curentIndexx <= (image_s_d.getRight())){
                curentIndexx += (image_s_d.getWidth()) / (25 * (rightIndex - leftIndex - 1));
                if(curentIndexx >= image_s_d.getRight()){
                    curentIndexx = image_s_d.getRight();
                    RelativeLayout.LayoutParams layoutParams =
                        (LayoutParams) view_index.getLayoutParams();
                    layoutParams.leftMargin = curentIndexx;
                    view_index.setLayoutParams(layoutParams);
                    view_index.setVisibility(INVISIBLE);
                }else{
                    RelativeLayout.LayoutParams layoutParams =
                        (LayoutParams) view_index.getLayoutParams();
                    layoutParams.leftMargin = curentIndexx;
                    view_index.setLayoutParams(layoutParams);
                    view_index.postDelayed(this, 40);
                }
            }else{
                view_index.setVisibility(INVISIBLE);
            }
        }
    };
    
    //初始化
    private void initView(Context context){
        View.inflate(context, R.layout.item_view, this);
        mRecyclerView = findViewById(R.id.recycler_view);
        view_index = findViewById(R.id.view_index);
        mTextViewCancel = findViewById(R.id.text_cancel);
        mTextViewSuccess = findViewById(R.id.text_success);
        view_video = findViewById(R.id.view_video);
        image_s_d = findViewById(R.id.image_s_d);
        view_video.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mediaPlayer){
                play(leftIndex * picSec);
            }
        });
        
        
        getWindowWidth((Activity) context);
        
        mRecyclerView.setOnTouchListener(this);
        mTextViewSuccess.setOnClickListener(this);
        mTextViewCancel.setOnClickListener(this);
        
    }
    
    private boolean isCanMove1 = false;
    private boolean isCanMove2 = false;
    
    
    // 屏幕宽度（像素）
    private void getWindowWidth(Activity context){
        DisplayMetrics metric = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(metric);
        with = metric.widthPixels;
    }
    
    @Override
    public boolean onTouch(View view, MotionEvent event){
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            if(Math.abs((int) event.getX() - image_s_d.getLeft()) < Math.abs((int) event.getX() - image_s_d.getRight())){
                isCanMove1 = true;
                isCanMove2 = false;
            }else{
                isCanMove1 = false;
                isCanMove2 = true;
            }
        }else if(event.getAction() == MotionEvent.ACTION_MOVE){
            if(isCanMove1){
                if((int) event.getX() > image_s_d.getRight() - 3 * childwith){
                    Toast.makeText(getContext(), "截取最小时间间隔为" + (3000) + "毫秒", Toast.LENGTH_SHORT).show();
                    return false;
                }
                leftIndex = ((int) event.getX() / childwith);
                view_index.setVisibility(INVISIBLE);
                play(leftIndex * picSec);
                if(rightIndex - leftIndex > maxDurtion / picSec){
                    Toast.makeText(getContext(), "截取最大时间间隔为" + (maxDurtion) + "毫秒",
                        Toast.LENGTH_SHORT).show();
                    return false;
                }
                RelativeLayout.LayoutParams layoutParams =
                    (LayoutParams) image_s_d.getLayoutParams();
                layoutParams.leftMargin = (int) event.getX();
                image_s_d.setLayoutParams(layoutParams);
            }else if(isCanMove2){
                if(event.getX() < image_s_d.getLeft() + 3 * childwith){
                    Toast.makeText(getContext(), "截取最小时间间隔为" + (3000) + "毫秒", Toast.LENGTH_SHORT).show();
                    return false;
                }
                rightIndex = ((int) event.getX() / childwith);
                if(rightIndex - leftIndex > maxDurtion / picSec){
                    Toast.makeText(getContext(), "截取最大时间间隔为" + (maxDurtion) + "毫秒",
                        Toast.LENGTH_SHORT).show();
                    return false;
                }
                RelativeLayout.LayoutParams layoutParams =
                    (LayoutParams) image_s_d.getLayoutParams();
                layoutParams.rightMargin = with - (int) event.getX();
                image_s_d.setLayoutParams(layoutParams);
            }
            
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            isCanMove1 = false;
            isCanMove2 = false;
        }
        if(isCanMove2 || isCanMove1){
            return true;
        }
        
        return false;
    }
    
    
    //播放
    private void play(int second){
        currentSec = second / picSec;
        if(view_video.getVisibility() == GONE){
            view_video.setVisibility(VISIBLE);
        }
        if(view_video.isPlaying()){
            view_video.seekTo(second);
        }else{
            view_video.stopPlayback();
            view_video.setVideoPath(path);
            view_video.start();
            view_video.seekTo(second);
        }
        removeCallbacks(mRunnable);
        postDelayed(mRunnable, 0);
        
        
        curentIndexx = image_s_d.getLeft();
        view_index.removeCallbacks(mRunnableIndex);
        view_index.postDelayed(mRunnableIndex, 1000);
        
        
    }
    
    private int pauseSec = 0;
    
    //继续播放
    public void onResume(){
        if(view_video != null && view_index.getVisibility() == VISIBLE){
            view_video.resume();
            play(pauseSec);
        }
    }
    
    
    //暂停播放
    public void onPause(){
        if(view_video != null){
            pauseSec = view_video.getCurrentPosition();
            view_video.pause();
            removeCallbacks(mRunnable);
            view_index.removeCallbacks(mRunnable);
        }
    }
    
    
    //销毁播放
    public void onDestroy(){
        if(view_video != null){
            view_video.pause();
            view_video.stopPlayback();
            view_video.setOnCompletionListener(null);
            removeCallbacks(mRunnable);
            view_index.removeCallbacks(mRunnable);
        }
    }
}
