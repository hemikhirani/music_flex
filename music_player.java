package com.example.flex_music;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.*;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.preference.PreferenceManager;

import com.example.flex_music.fragments.SongsFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class music_player extends AppCompatActivity {

    // UI Components
    ImageView imageViewAlbum;
    TextView textViewSongTitle, textViewCurrentTime, textViewTotalTime;
    SeekBar seekBar;
    ImageButton buttonPlayPause, buttonNext, buttonPrev, buttonLoop, buttonShuffle, buttonWishlist;

    // Media Player
    MediaPlayer mediaPlayer;
    Handler handler = new Handler();
    ArrayList<SongsFragment.Song> songs;
    int currentIndex = 0;
    boolean isLooping = false;
    boolean isShuffling = false;

    // Preferences & Favorites
    ArrayList<SongsFragment.Song> favoriteSongs;
    SharedPreferences prefs;
    Gson gson = new Gson();

    // Notification Components
    private static final String CHANNEL_ID = "flex_music_channel";
    private static final int NOTIFICATION_ID = 101;
    private MediaSessionCompat mediaSession;
    private NotificationManagerCompat notificationManager;
    private MediaService mediaService;
    private boolean isBound = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Setup dark mode
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        // Initialize UI Components
        initializeViews();
        setupMediaSession();
        createNotificationChannel();
        startMediaService();

        // Get intent data
        Intent intent = getIntent();
        songs = (ArrayList<SongsFragment.Song>) intent.getSerializableExtra("songList");
        currentIndex = intent.getIntExtra("songIndex", 0);

        if (songs == null || songs.isEmpty()) {
            Toast.makeText(this, "No songs to play!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize favorites
        String favJson = prefs.getString("favorites", "");
        favoriteSongs = favJson.isEmpty() ? new ArrayList<>() :
                gson.fromJson(favJson, new TypeToken<ArrayList<SongsFragment.Song>>(){}.getType());

        // Set click listeners
        setButtonListeners();
        setupSeekBar();

        // Start playback
        playSong(currentIndex);
        updateWishlistIcon();
    }

    private void initializeViews() {
        imageViewAlbum = findViewById(R.id.imageViewAlbum);
        textViewSongTitle = findViewById(R.id.textViewSongTitle);
        textViewCurrentTime = findViewById(R.id.textViewCurrentTime);
        textViewTotalTime = findViewById(R.id.textViewTotalTime);
        seekBar = findViewById(R.id.seekBar);
        buttonPlayPause = findViewById(R.id.buttonPlayPause);
        buttonNext = findViewById(R.id.buttonNext);
        buttonPrev = findViewById(R.id.buttonPrev);
        buttonLoop = findViewById(R.id.buttonLoop);
        buttonShuffle = findViewById(R.id.buttonShuffle);
        buttonWishlist = findViewById(R.id.buttonWishlist);
    }

    private void setButtonListeners() {
        buttonPlayPause.setOnClickListener(v -> togglePlayPause());
        buttonNext.setOnClickListener(v -> playNext());
        buttonPrev.setOnClickListener(v -> playPrevious());
        buttonLoop.setOnClickListener(v -> toggleLoop());
        buttonShuffle.setOnClickListener(v -> toggleShuffle());
        buttonWishlist.setOnClickListener(v -> toggleFavorite(currentIndex));
    }

    private void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    // Media Session and Notification Setup
    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(this, "FlexMusicSession");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlay() { togglePlayPause(); }
            @Override public void onPause() { togglePlayPause(); }
            @Override public void onSkipToNext() { playNext(); }
            @Override public void onSkipToPrevious() { playPrevious(); }
            @Override public void onStop() { stopSong(); }
        });
        mediaSession.setActive(true);
    }
    private void stopSong() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Controls",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Music playback controls");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        notificationManager = NotificationManagerCompat.from(this);
    }

    private void startMediaService() {
        Intent serviceIntent = new Intent(this, MediaService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaService.MediaBinder binder = (MediaService.MediaBinder) service;
            mediaService = binder.getService();
            isBound = true;
            mediaService.setMediaPlayer(mediaPlayer);
            mediaService.setCurrentSong(songs.get(currentIndex));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private void showNotification() {
        if (songs == null || songs.isEmpty()) return;

        SongsFragment.Song currentSong = songs.get(currentIndex);
        int playPauseIcon = mediaPlayer != null && mediaPlayer.isPlaying()
                ? R.drawable.pause : R.drawable.play;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(currentSong.getTitle())
                .setContentText("Flex Music")
                .setLargeIcon(getAlbumArt(currentSong))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.previous, "Previous", getActionIntent("ACTION_PREVIOUS"))
                .addAction(playPauseIcon, "Play/Pause", getActionIntent("ACTION_PLAY_PAUSE"))
                .addAction(R.drawable.next, "Next", getActionIntent("ACTION_NEXT"))
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private PendingIntent getActionIntent(String action) {
        Intent intent = new Intent(this, MediaReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private Bitmap getAlbumArt(SongsFragment.Song song) {
        // Implement actual album art loading from file
        return BitmapFactory.decodeResource(getResources(), R.drawable.logo);
    }

    // Media Player Methods
    private void playSong(int index) {
        stopSong();
        if (songs == null || songs.isEmpty()) return;

        SongsFragment.Song song = songs.get(index);
        textViewSongTitle.setText(song.getTitle());

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            buttonPlayPause.setImageResource(R.drawable.pause);
            seekBar.setMax(mediaPlayer.getDuration());
            textViewTotalTime.setText(formatTime(mediaPlayer.getDuration()));

            mediaPlayer.setOnCompletionListener(mp -> {
                if (isLooping) playSong(currentIndex);
                else playNext();
            });

            updateSeekBar();
            saveToRecentlyPlayed(song);
            showNotification();

        } catch (IOException e) {
            Toast.makeText(this, "Error playing song", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                buttonPlayPause.setImageResource(R.drawable.play);
            } else {
                mediaPlayer.start();
                buttonPlayPause.setImageResource(R.drawable.pause);
                updateSeekBar();
            }
            showNotification();
        }
    }

    private void playNext() {
        if (songs == null || songs.isEmpty()) return;
        if (isShuffling) Collections.shuffle(songs);
        currentIndex = (currentIndex + 1) % songs.size();
        playSong(currentIndex);
        updateWishlistIcon();
    }

    private void playPrevious() {
        if (songs == null || songs.isEmpty()) return;
        currentIndex = (currentIndex - 1 + songs.size()) % songs.size();
        playSong(currentIndex);
        updateWishlistIcon();
    }
    private void toggleLoop() {
        isLooping = !isLooping;
        buttonLoop.setAlpha(isLooping ? 1.0f : 0.5f);
    }

    private void toggleShuffle() {
        isShuffling = !isShuffling;
        buttonShuffle.setAlpha(isShuffling ? 1.0f : 0.5f);
    }

    private void toggleFavorite(int index) {
        SongsFragment.Song song = songs.get(index);
        if (isFavorite(song)) {
            favoriteSongs.removeIf(s -> s.getPath().equals(song.getPath()));
            Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
        } else {
            favoriteSongs.add(song);
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putString("favorites", gson.toJson(favoriteSongs)).apply();
        updateWishlistIcon();
    }

    private boolean isFavorite(SongsFragment.Song song) {
        for (SongsFragment.Song s : favoriteSongs) {
            if (s.getPath().equals(song.getPath())) return true;
        }
        return false;
    }

    private void updateWishlistIcon() {
        if (isFavorite(songs.get(currentIndex))) {
            buttonWishlist.setImageResource(R.drawable.whishlist);
        } else {
            buttonWishlist.setImageResource(R.drawable.b_whishlist);
        }
    }

    private void updateSeekBar() {
        if (mediaPlayer == null) return;
        seekBar.setProgress(mediaPlayer.getCurrentPosition());
        textViewCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
        handler.postDelayed(updateSeek, 500);
    }

    Runnable updateSeek = this::updateSeekBar;

    @SuppressLint("DefaultLocale")
    private String formatTime(int millis) {
        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(millis);
        int seconds = (int) (TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
        return String.format("%02d : %02d", minutes, seconds);
    }

    private void saveToRecentlyPlayed(SongsFragment.Song song) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();

        String json = prefs.getString("recently_played", "");
        ArrayList<SongsFragment.Song> recentSongs;

        if (!json.isEmpty()) {
            Type type = new TypeToken<ArrayList<SongsFragment.Song>>() {}.getType();
            recentSongs = gson.fromJson(json, type);
        } else {
            recentSongs = new ArrayList<>();
        }

        // Remove the song if it's already in the list to avoid duplicates
        recentSongs.removeIf(s -> s.getPath().equals(song.getPath()));

        // Add the song to the top of the list
        recentSongs.add(0, song);

        // Limit the size (e.g., keep only the latest 20 songs)
        if (recentSongs.size() > 20) {
            recentSongs = new ArrayList<>(recentSongs.subList(0, 20));
        }

        // Save the updated list back to SharedPreferences
        String updatedJson = gson.toJson(recentSongs);
        prefs.edit().putString("recently_played", updatedJson).apply();
    }


    @Override
    public void onBackPressed() {
        stopSong(); // stop media safely
        super.onBackPressed(); // go back to previous screen
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        stopSong();
        notificationManager.cancel(NOTIFICATION_ID);
        mediaSession.release();
    }
    public static class MediaService extends Service {
        private MediaPlayer mediaPlayer;
        private SongsFragment.Song currentSong;
        private final IBinder binder = new MediaBinder();

        class MediaBinder extends Binder {
            MediaService getService() { return MediaService.this; }
        }

        public void setMediaPlayer(MediaPlayer player) { this.mediaPlayer = player; }
        public void setCurrentSong(SongsFragment.Song song) { this.currentSong = song; }

        @Override
        public IBinder onBind(Intent intent) { return binder; }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(NOTIFICATION_ID, createNotification());
            return START_STICKY;
        }

        private Notification createNotification() {
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Music Playing")
                    .setSmallIcon(R.drawable.logo)
                    .build();
        }
    }

    // Broadcast Receiver Class
    public static class MediaReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent serviceIntent = new Intent(context, MediaService.class);
            String action = intent.getAction();

            if (action != null) {
                switch (action) {
                    case "ACTION_PLAY_PAUSE":
                        ((music_player) context).togglePlayPause();
                        break;
                    case "ACTION_NEXT":
                        ((music_player) context).playNext();
                        break;
                    case "ACTION_PREVIOUS":
                        ((music_player) context).playPrevious();
                        break;
                }
            }
        }
    }
}

