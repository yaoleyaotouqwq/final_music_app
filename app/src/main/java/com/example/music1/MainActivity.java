package com.example.music1;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.design.widget.BottomNavigationView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.security.PublicKey;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private  MusicService myService;
    private boolean myBound = false;

    private final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String [] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE

    };

    private ContentResolver mContentResolver;
    private ListView mPlaylist;
    private MediaCursorAdapter mCursorAdapter;

    private final String SELECTION =  MediaStore.Audio.Media.IS_MUSIC + " = ? " + " AND " + MediaStore.Audio.Media.MIME_TYPE + " LIKE ? ";
    private final String [] SELECTION_ARGS = {
            Integer . toString(1), "audio/mpeg"
    };

    public static final  String TAG="MainActivity";

    public static final String DATA_URI = "com.example.music1.DATA_URI";
    public static final String TITLE = "com.example.music1.TITLE";
    public static final String ARTIST = "com.example.music1.ARTIST";

    public static final String ACTION_MUSIC_START = "com.example.music1.ACTION_MUSIC_START";
    public static final String ACTION_MUSIC_STOP = "com.example.music1.ACTION_MUSIC_STOP";

    private BottomNavigationView navigation;
    private Boolean myPlayStatus = true;
    private ImageView Play;
    private TextView Bottom_Title;
    private TextView Bottom_Artist;
    private ImageView Bottom_Thumbnail;

    private MediaPlayer MediaPlayer = null;

    public static final int UPDATA_PROGRESS = 1;
    private ProgressBar myProgerss;

    private MusicReceiver musicReceiver;

    public class MusicReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(myService != null){
                myProgerss.setMax(myService.getDuration());
                new Thread(new MusicProgressRunnable()).start();
            }
        }
    }

    private Handler myHandler = new Handler(Looper.getMainLooper()){
      public void handleMessage(Message msg){
          switch (msg.what){
              case UPDATA_PROGRESS:
                  int position = msg.arg1;
                  myProgerss.setProgress(position);
                  break;
               default:
                   break;
          }
      }
    };

    private class MusicProgressRunnable implements Runnable{
        public MusicProgressRunnable(){

        }

        @Override
        public void run() {
            boolean myThreadWorking = true;
            while (myThreadWorking){
                try {
                    if(myService != null){
                        int position = myService.getCurrentPosition();
                        Message message = new Message();
                        message.what = UPDATA_PROGRESS;
                        message.arg1 = position;
                        myHandler.sendMessage(message);
                    }
                    myThreadWorking = myService.isPlaying();
                    Thread.sleep(100);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private ServiceConnection myConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            MusicService.MusicServiceBinder binder = (MusicService.MusicServiceBinder) iBinder;

            myService = binder.getService();
            myBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            myService = null;
            myBound = false;
        }
    };

    private ListView.OnItemClickListener itemClickListener = new ListView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView , View view, int i , long l) {

            Cursor cursor = mCursorAdapter.getCursor();
            if (cursor != null && cursor.moveToPosition(i )) {

                int titleIndex = cursor.getColumnIndex( MediaStore.Audio.Media.TITLE);
                int artistIndex = cursor.getColumnIndex(  MediaStore.Audio.Media.ARTIST);
                int albumIdIndex = cursor.getColumnIndex( MediaStore.Audio.Media.ALBUM_ID);
                int dataIndex = cursor.getColumnIndex(  MediaStore.Audio.Media.DATA);

                String title = cursor.getString(titleIndex );
                String artist = cursor.getString(artistIndex );
                Long albumId = cursor.getLong(albumIdIndex);
                String data = cursor.getString(dataIndex);

                Uri dataUri = Uri.parse(data);

                Intent serviceIntent = new Intent(MainActivity.this,  MusicService.class);
                serviceIntent.putExtra(MainActivity.DATA_URI,data);
                serviceIntent.putExtra(MainActivity.TITLE, title);
                serviceIntent.putExtra(MainActivity.ARTIST, artist);
                startService(serviceIntent);

                if (MediaPlayer != null) {
                    try { MediaPlayer.reset();
                        MediaPlayer.setDataSource(MainActivity.this , dataUri);
                        MediaPlayer.prepare();
                        MediaPlayer.start ();
                    } catch (IOException ex) {
                        ex.printStackTrace ();
                    }
                }

                navigation. setVisibility(View.VISIBLE);

                if (Bottom_Title != null) {
                    Bottom_Title.setText( title );
                }

                if (Bottom_Artist != null) {
                    Bottom_Artist.setText(artist );
                }

                Uri allbumUri = ContentUris.withAppendedId( MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId);
                Cursor allbumCursor = mContentResolver.query(  allbumUri,  null ,  null , null ,  null);
                if (allbumCursor != null && allbumCursor.getCount()>0 ) {
                    allbumCursor.moveToFirst();
                    int allbumArtIndex = allbumCursor.getColumnIndex(  MediaStore.Audio.Albums.ALBUM_ART);
                    String allbumArt = allbumCursor. getString(allbumArtIndex);
                    Glide.with(MainActivity.this).load(allbumArt).into(Bottom_Thumbnail);
                    allbumCursor.close();
                }

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContentResolver = getContentResolver();
        mCursorAdapter = new MediaCursorAdapter(MainActivity.this);
        mPlaylist = findViewById(R.id.play_list);
        mPlaylist.setAdapter(mCursorAdapter);

        navigation = findViewById(R.id.navigation );
        LayoutInflater.from(this).inflate(R.layout.music_buttom, navigation,true);
        Play = navigation.findViewById(R.id.play);
        Bottom_Title = navigation.findViewById(R.id.bottom_title);
        Bottom_Artist = navigation.findViewById(R.id.bottom_artist);
        Bottom_Thumbnail = navigation.findViewById(R.id.bottom_thumbnail);

        mPlaylist.setOnItemClickListener(itemClickListener);

        myProgerss = findViewById(R.id.progress);

        if (Play != null) {
            Play.setOnClickListener(MainActivity.this);
        }

        navigation. setVisibility(View.GONE);

        if (ContextCompat.checkSelfPermission(this , Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    MainActivity.this , Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                requestPermissions(PERMISSIONS_STORAGE,  REQUEST_EXTERNAL_STORAGE);
            }
        }
        else {
            initPlaylist ();
        }

        musicReceiver = new MusicReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_MUSIC_START);
        intentFilter.addAction(ACTION_MUSIC_STOP);
        registerReceiver(musicReceiver,intentFilter);

    }


    @Override
    protected void onDestroy(){
        unregisterReceiver(musicReceiver);
        super.onDestroy();
    }

    @Override public void onRequestPermissionsResult(int requestCode , String [] permissions , int [] grantResults) {

        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults [0] == PackageManager.PERMISSION_GRANTED) {
                    initPlaylist ();
                }
                break;
                default:
                    break;
        }
    }

    private void initPlaylist () {
        Cursor mCursor = mContentResolver.query(  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null , SELECTION, SELECTION_ARGS, MediaStore.Audio.Media.DEFAULT_SORT_ORDER  );
        mCursorAdapter.swapCursor(mCursor);
        mCursorAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(MainActivity.this,MusicService.class);
        bindService(intent,myConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        unbindService(myConn);
        myBound = false;
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.play){
            myPlayStatus = !myPlayStatus;
            if(myPlayStatus == true){
                myService.play();
                Play.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp);
            }
            else{
                myService.pause();
                Play.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
            }
        }
    }


}
