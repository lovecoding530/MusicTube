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

package com.bhagathsing.android.mytube.model;

import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.support.v4.media.MediaMetadataCompat;

import com.bhagathsing.android.mytube.ui.MusicPlayerActivity;
import com.bhagathsing.android.mytube.utils.LogHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Utility class to get a list of MusicTrack's based on a server-side JSON
 * configuration.
 */
public class MytubeSource implements MusicProviderSource {

    private static final String TAG = LogHelper.makeLogTag(MytubeSource.class);

    private static File ROOT_DIRECTORY;

    private static final String JSON_MUSIC = "music";
    private static final String JSON_TITLE = "title";
    private static final String JSON_ALBUM = "album";
    private static final String JSON_ARTIST = "artist";
    private static final String JSON_GENRE = "genre";
    private static final String JSON_SOURCE = "source";
    private static final String JSON_IMAGE = "image";
    private static final String JSON_TRACK_NUMBER = "trackNumber";
    private static final String JSON_TOTAL_TRACK_COUNT = "totalTrackCount";
    private static final String JSON_DURATION = "duration";


    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        ROOT_DIRECTORY = Environment.getExternalStoragePublicDirectory(MusicPlayerActivity.APP_NAME);
        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();
        if (ROOT_DIRECTORY.canRead()){
            for (File dirFile: ROOT_DIRECTORY.listFiles()){
                if(dirFile.isDirectory()){
                    for (File musicFile : dirFile.listFiles()){
                        if (musicFile.isFile()){
                            tracks.add(buildFromLocalFile(musicFile));
                        }
                    }
                }
            }
        }
        return tracks.iterator();
    }

    @Override
    public Iterable<String> categories() {
        ROOT_DIRECTORY = Environment.getExternalStoragePublicDirectory(MusicPlayerActivity.APP_NAME);
        ArrayList<String> categories = new ArrayList<>();
        if (ROOT_DIRECTORY.canRead()) {
            for (File dirFile : ROOT_DIRECTORY.listFiles()) {
                if (dirFile.isDirectory()) {
                    categories.add(dirFile.getName());
                }
            }
        }
        return categories;
    }

    private MediaMetadataCompat buildFromLocalFile(File file) {
        File parentFile = file.getParentFile();
        String filePath = file.getAbsolutePath();
        String title = file.getName().replaceFirst("[.][^.]+$", "");
        String album = parentFile.getName();
        String artist = "unknown";
        String genre = parentFile.getName();
        String source = filePath;
        String iconUrl = "android.resource://com.bhagathsing.android.mytube/drawable/ic_default_art.png";
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(filePath);
        String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        mmr.release();

        String id = String.valueOf(filePath.hashCode());

        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, source)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.parseLong(duration))
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .build();
    }
}
