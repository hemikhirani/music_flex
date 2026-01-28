package com.example.flex_music;

import android.app.Service;
import android.content.Intent;
import android.media.session.MediaSession;
import android.os.IBinder;

public class MediaService extends Service {
    private MediaSession mediaSession;

    public class MediaBinder extends android.os.Binder {
        MediaService getService() {
            return MediaService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSession(this, "FlexMusicService");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mediaSession.setActive(true);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mediaSession.setActive(false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MediaBinder();
    }

    public MediaSession.Token getSessionToken() {
        return mediaSession.getSessionToken();
    }
}