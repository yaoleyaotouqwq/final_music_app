package com.example.music1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class MusicService extends Service {

    MediaPlayer myMediaPlayer;
    private static final int ONGOING_NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "Music channel";
    NotificationManager myNotificationManager;

    private final IBinder myBinder = new MusicServiceBinder();

    public class MusicServiceBinder extends Binder{
        MusicService getService(){
            return MusicService.this;
        }
    }
    public MusicService() {
    }

    @Override public void onDestroy() {
        myMediaPlayer.stop();
        myMediaPlayer. release ();
        myMediaPlayer = null;
        super.onDestroy();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        myMediaPlayer = new MediaPlayer();
    }

    @Override public int onStartCommand(Intent intent , int flags , int startId) {
        String data = intent.getStringExtra( MainActivity.DATA_URI);

        Uri dataUri = Uri.parse(data);
        if (myMediaPlayer != null) {
            try {
                myMediaPlayer.reset ();
                myMediaPlayer.setDataSource(getApplicationContext() , dataUri);
                myMediaPlayer.prepare();
                myMediaPlayer.start ();
                Intent musicStartintent = new Intent(MainActivity.ACTION_MUSIC_START);
                sendBroadcast(musicStartintent);

            } catch (IOException ex) {
                ex.printStackTrace ();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            myNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Music Channel", NotificationManager.IMPORTANCE_HIGH);
            if (myNotificationManager != null) {
                myNotificationManager.createNotificationChannel(channel);
            }
        }

        Intent notificationIntent = new Intent(getApplicationContext() , MainActivity.class);
        PendingIntent pendingIntent =  PendingIntent.getActivity( getApplicationContext() , 0, notificationIntent , 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext() ,CHANNEL_ID);
        Notification notification = builder.setContentTitle( notificationIntent.getStringExtra(MainActivity.TITLE) ).setContentText(notificationIntent.getStringExtra(MainActivity.ARTIST)).setSmallIcon(R.drawable.ic_launcher_foreground).setContentIntent(pendingIntent).build ();
        startForeground(ONGOING_NOTIFICATION_ID, notification );


        return super.onStartCommand(intent , flags , startId );
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return myBinder;
    }

    public void pause(){
        if(myMediaPlayer != null && myMediaPlayer.isPlaying()){
            myMediaPlayer.pause();
        }
    }

    public void play(){
        if(myMediaPlayer != null){
            myMediaPlayer.start();
        }
    }

    public int getDuration() {
        int duration = 0;

        if(myMediaPlayer != null){
            duration = myMediaPlayer.getDuration();
        }

        return duration;
    }

    public int getCurrentPosition(){
        int position = 0;
        if(myMediaPlayer != null){
            position = myMediaPlayer.getCurrentPosition();
        }

        return position;
    }

    public boolean isPlaying(){

        if(myMediaPlayer != null){
            return myMediaPlayer.isPlaying();
        }
        return false;
    }
}
