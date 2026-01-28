package com.example.flex_music;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flex_music.Adapter.SongAdapter;
import com.example.flex_music.fragments.SongsFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private ArrayList<SongsFragment.Song> favoritesList;
    private SharedPreferences prefs;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        recyclerView = findViewById(R.id.recyclerViewFavorites);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        loadFavorites();
        adapter = new SongAdapter(favoritesList, this);
        recyclerView.setAdapter(adapter);
    }

    private void loadFavorites() {
        String json = prefs.getString("favorites", "");
        if (!json.isEmpty()) {
            favoritesList = gson.fromJson(json, new TypeToken<ArrayList<SongsFragment.Song>>() {}.getType());
        } else {
            favoritesList = new ArrayList<>();
        }
    }
}
