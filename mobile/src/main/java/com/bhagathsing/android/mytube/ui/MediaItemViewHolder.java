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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bhagathsing.android.mytube.R;
import com.bhagathsing.android.mytube.model.MusicProvider;
import com.bhagathsing.android.mytube.model.MutableMediaMetadata;
import com.bhagathsing.android.mytube.model.MytubeSource;
import com.bhagathsing.android.mytube.utils.MediaIDHelper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentMap;

public class MediaItemViewHolder {

    public static final int STATE_INVALID = -1;
    public static final int STATE_NONE = 0;
    public static final int STATE_PLAYABLE = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_PLAYING = 3;

    private static ColorStateList sColorStatePlaying;
    private static ColorStateList sColorStateNotPlaying;

    private ImageView mImageView;
    private TextView mTitleView;
    private TextView mDescriptionView;
    private ImageButton mFavButton;

    // Returns a view for use in media item list.
    static View setupListView(final Activity activity, View convertView, ViewGroup parent,
                              final MediaBrowserCompat.MediaItem item) {

        if (sColorStateNotPlaying == null || sColorStatePlaying == null) {
            initializeColorStateLists(activity);
        }

        MediaItemViewHolder holder;

        Integer cachedState = STATE_INVALID;

        if (convertView == null) {
            convertView = LayoutInflater.from(activity)
                    .inflate(R.layout.media_list_item, parent, false);
            holder = new MediaItemViewHolder();
            holder.mImageView = (ImageView) convertView.findViewById(R.id.play_eq);
            holder.mTitleView = (TextView) convertView.findViewById(R.id.title);
//            holder.mDescriptionView = (TextView) convertView.findViewById(R.id.description);
            holder.mFavButton = (ImageButton) convertView.findViewById(R.id.imageButton);
            convertView.setTag(holder);
        } else {
            holder = (MediaItemViewHolder) convertView.getTag();
            cachedState = (Integer) convertView.getTag(R.id.tag_mediaitem_state_cache);
        }



        MediaDescriptionCompat description = item.getDescription();
        holder.mTitleView.setText(description.getTitle());
//        holder.mDescriptionView.setText(description.getSubtitle());

        holder.mFavButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(activity);
                builderSingle.setIcon(R.drawable.ic_launcher);
                builderSingle.setTitle("Select favorite.");

                ArrayList<String> categories = MytubeSource.getCategories();
                categories.remove(0);

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        activity,
                        R.layout.select_dialog_single_choice,
                        categories);

                builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String strCategory = arrayAdapter.getItem(which);
                        String mediaId = MediaIDHelper.extractMusicIDFromMediaID(item.getMediaId());
                        MediaMetadataCompat metadata = MusicProvider.mMusicListById.get(mediaId).metadata;
                        MytubeSource.insertMusic(strCategory, metadata);
                    }
                });
                builderSingle.show();
            }
        });
        // If the state of convertView is different, we need to adapt the view to the
        // new state.
        int state = getMediaItemState(activity, item);
        if (cachedState == null || cachedState != state) {
            final Drawable drawable = getDrawableByState(activity, state);
            if (drawable != null) {
                holder.mImageView.setImageDrawable(drawable);
                holder.mImageView.setVisibility(View.VISIBLE);

                if (drawable.getClass() == AnimationDrawable.class){
                    holder.mImageView.post(new Runnable() {

                        @Override
                        public void run() {
                            ((AnimationDrawable)drawable).start();
                        }
                    });
                }
            }
            else {
//                holder.mImageView.setVisibility(View.GONE);
                holder.mImageView.setImageResource(R.drawable.apple_music);
            }
            convertView.setTag(R.id.tag_mediaitem_state_cache, state);
        }

        if(activity.getClass() != SearchableActivity.class){
            holder.mFavButton.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    private static void initializeColorStateLists(Context ctx) {
        sColorStateNotPlaying = ColorStateList.valueOf(ctx.getResources().getColor(
            R.color.media_item_icon_not_playing));
        sColorStatePlaying = ColorStateList.valueOf(ctx.getResources().getColor(
            R.color.media_item_icon_playing));
    }

    public static Drawable getDrawableByState(Context context, int state) {
        if (sColorStateNotPlaying == null || sColorStatePlaying == null) {
            initializeColorStateLists(context);
        }

        switch (state) {
            case STATE_PLAYABLE:
                Drawable pauseDrawable = ContextCompat.getDrawable(context,
                        R.drawable.ic_play_arrow_black_36dp);
                DrawableCompat.setTintList(pauseDrawable, sColorStateNotPlaying);
                return pauseDrawable;
            case STATE_PLAYING:
                AnimationDrawable animation = (AnimationDrawable)
                        ContextCompat.getDrawable(context, R.drawable.ic_equalizer_white_36dp);
                DrawableCompat.setTintList(animation, sColorStatePlaying);
//                animation.start();
                return animation;
            case STATE_PAUSED:
                Drawable playDrawable = ContextCompat.getDrawable(context,
                        R.drawable.ic_equalizer1_white_36dp);
                DrawableCompat.setTintList(playDrawable, sColorStatePlaying);

                AnimationDrawable loadingAnimation = (AnimationDrawable)
                        ContextCompat.getDrawable(context, R.drawable.loading);
                DrawableCompat.setTintList(loadingAnimation, sColorStatePlaying);
//                loadingAnimation.start();
                return loadingAnimation;
            default:
                return null;
        }
    }

    public static int getMediaItemState(Activity context, MediaBrowserCompat.MediaItem mediaItem) {
        int state = STATE_NONE;
        // Set state to playable first, then override to playing or paused state if needed
        if (mediaItem.isPlayable()) {
            state = STATE_PLAYABLE;
            if (MediaIDHelper.isMediaItemPlaying(context, mediaItem)) {
                state = getStateFromController(context);
            }
        }

        return state;
    }

    public static int getStateFromController(Activity context) {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(context);
        PlaybackStateCompat pbState = controller.getPlaybackState();
        if (pbState == null ||
                pbState.getState() == PlaybackStateCompat.STATE_ERROR) {
            return MediaItemViewHolder.STATE_NONE;
        } else if (pbState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            return  MediaItemViewHolder.STATE_PLAYING;
        } else {
            return MediaItemViewHolder.STATE_PAUSED;
        }
    }
}
