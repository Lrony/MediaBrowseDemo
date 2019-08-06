package com.lrony.mediabrowsedemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lrony.mediabrowsedemo.utils.MediaIDHelper;
import com.lrony.mediabrowsedemo.utils.PermissionReq;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@SuppressLint("NewApi")
public class MainActivity extends AppCompatActivity implements MainInterface,
        MainAdapter.OnItemClickListener,
        View.OnClickListener {

    private static final String TAG = "MainActivity";

    private static final int MSG_UPDATE_AUDIO_PROGRESS = 0;
    private static final int MSG_PLAY_BACK_PLAY_PAUSE = 1;

    private MainModel mModel;

    private RecyclerView mListMain;
    private MainAdapter mAdapter;

    private LinearLayout mLlPlayback;
    private ProgressBar mProgressBar;
    private ImageView mImgIcon;
    private TextView mTvTitle;
    private ImageView mImgPlayPause;

    private List<MediaBrowser.MediaItem> mMediaItems = new ArrayList<>();

    private Stack<String> mStack = new Stack<>();

    private MainHandler mMainHandler = new MainHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        mModel = new MainModel(this, this);
        mModel.mediaBrowserConnect();

        initView();
        initListener();
        initNotificationChannel();

        PermissionReq.with(this)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .result(new PermissionReq.Result() {
                    @Override
                    public void onGranted() {
                        Log.d(TAG, "onGranted");
                        mStack.push(MediaIDHelper.MEDIA_ID_ROOT);
                        mModel.requestData(MediaIDHelper.MEDIA_ID_ROOT);
                    }

                    @Override
                    public void onDenied() {
                        Log.d(TAG, "onDenied");
                        Toast.makeText(MainActivity.this, "No Permission", Toast.LENGTH_SHORT).show();
                    }
                }).request();
    }

    private void initView() {
        Log.d(TAG, "initView");
        mAdapter = new MainAdapter(this, mMediaItems);
        mListMain = findViewById(R.id.list_main);
        mListMain.setLayoutManager(new LinearLayoutManager(this));
        mListMain.setAdapter(mAdapter);

        mLlPlayback = findViewById(R.id.ll_playback);

        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setMax(1000);

        mImgIcon = findViewById(R.id.img_icon);
        mTvTitle = findViewById(R.id.tv_title);
        mImgPlayPause = findViewById(R.id.img_play_pause);
    }

    private void initListener() {
        Log.d(TAG, "initListener");
        mAdapter.setOnItemClickListener(this);
        mLlPlayback.setOnClickListener(this);
        mImgPlayPause.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mMainHandler.sendEmptyMessage(MSG_UPDATE_AUDIO_PROGRESS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mMainHandler.removeMessages(MSG_UPDATE_AUDIO_PROGRESS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mModel.mediaBrowserDisconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionReq.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void updateAudioList(String parentId, List<MediaBrowser.MediaItem> list) {
        Log.d(TAG, "updateAudioList parentId: " + parentId + " ,size: " + list.size());
        mMediaItems.clear();
        mMediaItems.addAll(list);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onMediaBrowserConnected(MediaController mediaController) {
        Log.d(TAG, "onMediaBrowserConnected");
        if (mediaController != null) {
            setMediaController(mediaController);
            updatePlayPauseButtonImage(mediaController.getPlaybackState());
        }
    }

    @Override
    public void onMediaBrowserConnectionSuspended() {
        Log.d(TAG, "onMediaBrowserConnectionSuspended");
        setMediaController(null);
    }

    @Override
    public void onPlaybackStateChanged(PlaybackState state) {
        Log.d(TAG, "onPlaybackStateChanged");
        updateProgressBar();
        updatePlayPauseButtonImage(state);
    }

    @SuppressLint("HandlerLeak")
    private class MainHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_AUDIO_PROGRESS:
                    long delay = updateProgressBar();
                    mMainHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_PROGRESS, delay);
                    break;
                case MSG_PLAY_BACK_PLAY_PAUSE:
                    mModel.playPause();
                    break;
                default:
                    break;
            }
        }
    }

    private void updatePlayPauseButtonImage(PlaybackState state) {
        Log.d(TAG, "updatePlayPauseButtonImage");
        if (state == null) {
            Log.d(TAG, "updatePlayPauseButtonImage state is null!!!");
            return;
        }
        if (state.getState() != PlaybackState.STATE_PLAYING) {
            mImgPlayPause.setImageResource(R.drawable.ic_play_arrow);
        } else {
            mImgPlayPause.setImageResource(R.drawable.ic_pause);
        }
    }

    private long updateProgressBar() {
        long duration = mModel.getDuration();
        long pos = mModel.getPosition();
        if (duration <= 0) {
            return 1000;
        }
        if (pos >= 0) {
            int progress = (int) (1000 * pos / duration);
            mProgressBar.setProgress(progress);
        } else {
            mProgressBar.setProgress(1000);
        }
        // calculate the number of milliseconds until the next full second, so
        // the counter can be updated at just the right time
        long remaining = 1000 - (pos % 1000);
        long smoothrefreshtime = duration / 320;
        if (smoothrefreshtime > remaining) {
            return remaining;
        }
        if (smoothrefreshtime < 20) {
            return 20;
        }
        return smoothrefreshtime;
    }

    @Override
    public void onMetadataChanged(MediaMetadata metadata) {
        Log.d(TAG, "onMetadataChanged");
        if (metadata == null) {
            Log.d(TAG, "onMetadataChanged metadata is null !!!");
            return;
        }
        String audioTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        Bitmap albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        mTvTitle.setText(audioTitle);
        mImgIcon.setImageBitmap(albumArt);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mStack.size() > 1) {
                mStack.pop();
                mModel.requestData(mStack.peek());
                return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onItemClick(View view, int pos) {
        Log.d(TAG, "onItemClick pos: " + pos);
        MediaBrowser.MediaItem mediaItem = mMediaItems.get(pos);
        if (mediaItem.isBrowsable()) {
            String mediaId = mediaItem.getMediaId();
            mStack.push(mediaId);
            mModel.requestData(mediaId);
        } else {
            mModel.playFromMediaId(mediaItem.getMediaId(), null);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_playback:
                startActivity(new Intent(this, PlaybackActivity.class));
                break;
            case R.id.img_play_pause:
                mMainHandler.sendEmptyMessage(MSG_PLAY_BACK_PLAY_PAUSE);
                break;
            default:
                break;
        }
    }

    private void initNotificationChannel() {
        Log.d(TAG, "initNotificationChannel");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "music";
            String channelName = "Music";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            createNotificationChannel(channelId, channelName, importance);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName, int importance) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }
}
