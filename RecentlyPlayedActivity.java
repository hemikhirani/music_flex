package com.example.flex_music;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flex_music.Adapter.SongAdapter;
import com.example.flex_music.fragments.SongsFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class RecentlyPlayedActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private ArrayList<SongsFragment.Song> recentlyPlayedList = new ArrayList<>();
    private SharedPreferences prefs;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recently_played);

        recyclerView = findViewById(R.id.recyclerViewRecentlyPlayed);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        loadRecentlyPlayedSongs();
        adapter = new SongAdapter(recentlyPlayedList, this);
        recyclerView.setAdapter(adapter);
    }

    private void loadRecentlyPlayedSongs() {
        String json = prefs.getString("recently_played", "");
        recentlyPlayedList.clear();

        if (!json.isEmpty()) {
            Type type = new TypeToken<ArrayList<SongsFragment.Song>>() {}.getType();
            ArrayList<SongsFragment.Song> allSongs = gson.fromJson(json, type);

            long now = System.currentTimeMillis();
            long twentyFourHoursAgo = now - (24 * 60 * 60 * 1000);

            for (SongsFragment.Song song : allSongs) {
                if (song.getLastPlayedTime() >= twentyFourHoursAgo) {
                    recentlyPlayedList.add(song);
                }
            }
        }
    }
}
