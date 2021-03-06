/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bhagathsing.android.mytube.ui;

import android.Manifest;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.TextUtils;
import android.util.Log;

import com.bhagathsing.android.mytube.R;
import com.bhagathsing.android.mytube.model.MusicProvider;
import com.bhagathsing.android.mytube.model.MutableMediaMetadata;
import com.bhagathsing.android.mytube.model.MytubeSource;
import com.bhagathsing.android.mytube.model.YoutubeAPIActivity;
import com.bhagathsing.android.mytube.utils.LogHelper;
import com.bhagathsing.android.mytube.utils.MediaIDHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.ArrayList;
import java.util.List;

import static com.bhagathsing.android.mytube.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE;

/**
 * Main activity for the music player.
 * This class hold the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop. Thus, a MediaBrowser will be always
 * connected while this activity is running.
 */
public class MusicPlayerActivity extends BaseActivity
        implements MediaBrowserFragment.MediaFragmentListener {

    private static final String TAG = LogHelper.makeLogTag(MusicPlayerActivity.class);
    private static final String SAVED_MEDIA_ID="com.bhagathsing.android.mytube.MEDIA_ID";
    private static final String FRAGMENT_TAG = "uamp_list_container";
    public static String APP_NAME;

    public static final String EXTRA_START_FULLSCREEN =
            "com.bhagathsing.android.mytube.EXTRA_START_FULLSCREEN";

    /**
     * Optionally used with {@link #EXTRA_START_FULLSCREEN} to carry a MediaDescription to
     * the {@link FullScreenPlayerActivity}, speeding up the screen rendering
     * while the {@link android.support.v4.media.session.MediaControllerCompat} is connecting.
     */
    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
        "com.bhagathsing.android.mytube.CURRENT_MEDIA_DESCRIPTION";

    public static String NEW_RECENTLY_SONGS = "New and recently played songs";
    public static String My_FAVORITE = "My favorite";
    public static String selectedCategory = "";
    public static ArrayList<SearchableActivity> searchableActivities;

    private Bundle mVoiceSearchParams;
    private  Bundle savedInstanceState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        setContentView(R.layout.activity_player);

        searchableActivities = new ArrayList<>();

        if (APP_NAME == null){
            APP_NAME = getResources().getString(R.string.app_name);
        }

        initializeToolbar();
        this.savedInstanceState = savedInstanceState;
        initializeFromParams(savedInstanceState, getIntent());

        // Only check if a full screen player is needed on the first time:
        if (savedInstanceState == null) {
            startFullScreenActivityIfNeeded(getIntent());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.e("Permission grant","You have permission");
            }else{
                Log.e("Permission error","You have asked for permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }else{
            MytubeSource.jsonFile.getParentFile().mkdirs();
            MytubeSource.insertCategory(NEW_RECENTLY_SONGS);
            MytubeSource.insertCategory(My_FAVORITE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != -1){
            MytubeSource.jsonFile.getParentFile().mkdirs();
            MytubeSource.insertCategory(NEW_RECENTLY_SONGS);
            MytubeSource.insertCategory(My_FAVORITE);
            super.mMediaBrowser.disconnect();
            super.mMediaBrowser.connect();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        String mediaId = getMediaId();
        if (mediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mediaId);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMediaItemSelected(MediaBrowserCompat.MediaItem item) {
        Log.d("Kangtle", "onMediaItemSelected, mediaId=" + item.getMediaId());

        if(item.getDescription().getTitle().equals(NEW_RECENTLY_SONGS)){
            if(MusicProvider.mMusicListByGenre.containsKey(NEW_RECENTLY_SONGS) &&
                    MusicProvider.mMusicListByGenre.get(NEW_RECENTLY_SONGS).size() >= YoutubeAPIActivity.SEARCH_MAX_RESULTS_NUM){
                String mediaId = "__BY_GENRE__/" + NEW_RECENTLY_SONGS;
                navigateToBrowser(mediaId);
            }else{
                Intent youtubeAPIIntent = new Intent(this, YoutubeAPIActivity.class);
                youtubeAPIIntent.putExtra("query", "New Songs");
                youtubeAPIIntent.putExtra("genre", NEW_RECENTLY_SONGS);
                startActivityForResult(youtubeAPIIntent, 0);
            }
        }else{

            if (item.isPlayable()) {
                MediaControllerCompat.getMediaController(MusicPlayerActivity.this).getTransportControls()
                        .playFromMediaId(item.getMediaId(), null);
                String mediaId = MediaIDHelper.extractMusicIDFromMediaID(item.getMediaId());
                MediaMetadataCompat metadata = MusicProvider.mMusicListById.get(mediaId).metadata;
                MytubeSource.insertMusic(NEW_RECENTLY_SONGS, metadata);
            } else if (item.isBrowsable()) {
                navigateToBrowser(item.getMediaId());
            } else {
                LogHelper.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: ",
                        "mediaId=", item.getMediaId());
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == 1){
            Log.d("Kangtle", String.valueOf(resultCode));
            MusicProvider.mMusicListById.putAll(YoutubeAPIActivity.searchResults);
            List<MediaMetadataCompat> list = MusicProvider.mMusicListByGenre.get(NEW_RECENTLY_SONGS);
            if (list == null) {
                list = new ArrayList<>();
                MusicProvider.mMusicListByGenre.put(NEW_RECENTLY_SONGS, list);
            }
            for (MutableMediaMetadata m: YoutubeAPIActivity.searchResults.values()){
                if(!list.contains(m.metadata))
                    list.add(m.metadata);
            }
            String mediaId = "__BY_GENRE__/" + NEW_RECENTLY_SONGS;
            navigateToBrowser(mediaId);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        LogHelper.d(TAG, "Setting toolbar title to ", title);
        if (title == null) {
            title = getString(R.string.app_name);
        }
        setTitle(title);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LogHelper.d(TAG, "onNewIntent, intent=" + intent);
        initializeFromParams(null, intent);
        startFullScreenActivityIfNeeded(intent);
    }

    private void startFullScreenActivityIfNeeded(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_START_FULLSCREEN, false)) {
            Intent fullScreenIntent = new Intent(this, FullScreenPlayerActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION,
                    intent.getParcelableExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION));
            startActivity(fullScreenIntent);
        }
    }

    protected void initializeFromParams(Bundle savedInstanceState, Intent intent) {
        String mediaId = MEDIA_ID_MUSICS_BY_GENRE;
        // check if we were started from a "Play XYZ" voice search. If so, we save the extras
        // (which contain the query details) in a parameter, so we can reuse it later, when the
        // MediaSession is connected.
        Log.d("Kangtle", "init player");
        if (intent.getAction() != null
            && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            mVoiceSearchParams = intent.getExtras();
            LogHelper.d(TAG, "Starting from voice search query=",
                mVoiceSearchParams.getString(SearchManager.QUERY));
        } else {
            if (savedInstanceState != null) {
                // If there is a saved media ID, use it
                mediaId = savedInstanceState.getString(SAVED_MEDIA_ID);
            }
        }
        navigateToBrowser(mediaId);
    }

    private void navigateToBrowser(String mediaId) {
        String[] parts = mediaId.split("/");
        if (parts.length > 1){
            selectedCategory = parts[1];
        }
        Log.d(TAG, "navigateToBrowser, mediaId=" + mediaId);
        MediaBrowserFragment fragment = getBrowseFragment();

        if (fragment == null || !TextUtils.equals(fragment.getMediaId(), mediaId)) {
            fragment = new MediaBrowserFragment();
            fragment.setMediaId(mediaId);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                R.animator.slide_in_from_left, R.animator.slide_out_to_right);
            transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
            // If this is not the top level media (root), we add it to the fragment back stack,
            // so that actionbar toggle and Back will work appropriately:
            if (!mediaId.equals(MEDIA_ID_MUSICS_BY_GENRE)) {
                transaction.addToBackStack(null);
            }
            transaction.commit();
        }
    }

    public String getMediaId() {
        MediaBrowserFragment fragment = getBrowseFragment();
        if (fragment == null) {
            return null;
        }
        return fragment.getMediaId();
    }

    private MediaBrowserFragment getBrowseFragment() {
        return (MediaBrowserFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    }

    @Override
    protected void onMediaControllerConnected() {
        if (mVoiceSearchParams != null) {
            // If there is a bootstrap parameter to start from a search query, we
            // send it to the media session and set it to null, so it won't play again
            // when the activity is stopped/started or recreated:
            String query = mVoiceSearchParams.getString(SearchManager.QUERY);
            MediaControllerCompat.getMediaController(MusicPlayerActivity.this).getTransportControls()
                    .playFromSearch(query, mVoiceSearchParams);
            mVoiceSearchParams = null;
        }
        getBrowseFragment().onConnected();
    }
}
