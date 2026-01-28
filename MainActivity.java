package com.example.flex_music;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.example.flex_music.Adapter.ViewPagerAdapter;
import com.example.flex_music.fragments.AlbumsFragment;
import com.example.flex_music.fragments.ArtistsFragment;
import com.example.flex_music.fragments.ListFragment;
import com.example.flex_music.fragments.SongsFragment;
import com.example.flex_music.utils.Searchable;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    TabLayout tabLayout;
    ViewPager viewPager;
    SearchView searchView;
    ImageView settingsIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchView = findViewById(R.id.searchView);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        settingsIcon = findViewById(R.id.settingsIcon);

        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);

        settingsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AppAppearanceActivity.class);
            startActivity(intent);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchInCurrentTab(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchInCurrentTab(newText);
                return true;
            }

            private void searchInCurrentTab(String query) {
                int position = viewPager.getCurrentItem();
                Fragment fragment = getSupportFragmentManager()
                        .findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + position);

                if (fragment instanceof Searchable) {
                    ((Searchable) fragment).onSearchQuery(query);
                }
            }
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new ListFragment(), "List");
        adapter.addFragment(new SongsFragment(), "Songs");
        adapter.addFragment(new ArtistsFragment(), "Artists");
        adapter.addFragment(new AlbumsFragment(), "Album");
        viewPager.setAdapter(adapter);
    }
}
