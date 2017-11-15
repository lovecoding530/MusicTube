package com.bhagathsing.android.mytube.ui;

import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.TextUtils;
import android.util.Log;

import com.bhagathsing.android.mytube.model.MusicProvider;
import com.bhagathsing.android.mytube.model.MutableMediaMetadata;
import com.bhagathsing.android.mytube.model.MytubeSource;
import com.bhagathsing.android.mytube.model.YoutubeAPIActivity;
import com.bhagathsing.android.mytube.utils.LogHelper;
import com.bhagathsing.android.mytube.R;
import com.bhagathsing.android.mytube.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.List;

public class SearchableActivity extends BaseActivity
        implements MediaBrowserFragment.MediaFragmentListener {

    private String query;

    /**
     * Create the main activity.
     */
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MusicPlayerActivity.searchableActivities.add(this);

        setContentView(R.layout.activity_searchable);
        initializeToolbar();
        showHomeButton();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
            Intent youtubeAPIIntent = new Intent(this, YoutubeAPIActivity.class);
            youtubeAPIIntent.putExtra("query", query);
            startActivityForResult(youtubeAPIIntent, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == 1){
            Log.d("Kangtle", String.valueOf(resultCode));
            MusicProvider.mMusicListById.putAll(YoutubeAPIActivity.searchResults);
            MusicProvider.buildListsByGenre();
            String mediaId = "__BY_GENRE__/Search_" + query;
            navigateToBrowser(mediaId);
        }
    }

    private static final String FRAGMENT_TAG = "uamp_list_container";

    private void navigateToBrowser(String mediaId) {
//        String[] parts = mediaId.split("/");
//        if (parts.length > 1){
//            selectedCategory = parts[1];
//        }
        MediaBrowserFragment fragment = getBrowseFragment();

        if (fragment == null || !TextUtils.equals(fragment.getMediaId(), mediaId)) {
            fragment = new MediaBrowserFragment();
            fragment.setMediaId(mediaId);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                    R.animator.slide_in_from_left, R.animator.slide_out_to_right);
            transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
            transaction.commitAllowingStateLoss();
        }
    }

    private MediaBrowserFragment getBrowseFragment() {
        return (MediaBrowserFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    }

    @Override
    public void onMediaItemSelected(MediaBrowserCompat.MediaItem item) {
        if (item.isPlayable()) {
            MediaControllerCompat.getMediaController(this).getTransportControls()
                    .playFromMediaId(item.getMediaId(), null);
            String mediaId = MediaIDHelper.extractMusicIDFromMediaID(item.getMediaId());
            MediaMetadataCompat metadata = MusicProvider.mMusicListById.get(mediaId).metadata;
            MytubeSource.insertMusic(MusicPlayerActivity.NEW_RECENTLY_SONGS, metadata);
        } else if (item.isBrowsable()) {
            navigateToBrowser(item.getMediaId());
        } else {
            LogHelper.w("Searchable Activity", "Ignoring MediaItem that is neither browsable nor playable: ",
                    "mediaId=", item.getMediaId());
        }
    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        if (title == null) {
            title = getString(R.string.app_name);
        }
        setTitle(title);
    }

    @Override
    protected void onMediaControllerConnected() {
        if(getBrowseFragment() != null)
            getBrowseFragment().onConnected();
    }
}

