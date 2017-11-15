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

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.bhagathsing.android.mytube.MyApplication;
import com.bhagathsing.android.mytube.ui.MusicPlayerActivity;
import com.bhagathsing.android.mytube.utils.LogHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class to get a list of MusicTrack's based on a server-side JSON
 * configuration.
 */
public class MytubeSource implements MusicProviderSource {

    private static final String TAG = LogHelper.makeLogTag(RemoteJSONSource.class);

    protected static final String CATALOG_URL =
            "http://storage.googleapis.com/automotive-media/music.json";

    private static final String JSON_MUSIC = "music";
    private static final String JSON_CATEGORIES = "categories";
    private static final String JSON_TITLE = "title";
    private static final String JSON_ALBUM = "album";
    private static final String JSON_ARTIST = "artist";
    private static final String JSON_GENRE = "genre";
    private static final String JSON_SOURCE = "source";
    private static final String JSON_IMAGE = "image";
    private static final String JSON_TRACK_NUMBER = "trackNumber";
    private static final String JSON_TOTAL_TRACK_COUNT = "totalTrackCount";
    private static final String JSON_DURATION = "duration";
    public static File jsonFile = Environment.getExternalStoragePublicDirectory(MusicPlayerActivity.APP_NAME+"/musictube.json");

    static {
        jsonFile.getParentFile().mkdirs();
    }

    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();
        try {
            JSONObject jsonObj = readJSON();
            if (jsonObj != null) {
                JSONArray jsonTracks = jsonObj.getJSONArray(JSON_MUSIC);

                if (jsonTracks != null) {
                    for (int j = 0; j < jsonTracks.length(); j++) {
                        tracks.add(buildFromJSON(jsonTracks.getJSONObject(j)));
                    }
                }
            }
        } catch (JSONException e) {
            LogHelper.e(TAG, e, "Could not retrieve music list");
        }

        Collections.sort(tracks, new Comparator<MediaMetadataCompat>() {
            @Override
            public int compare(MediaMetadataCompat o1, MediaMetadataCompat o2) {
                String title1 = o1.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
                String title2 = o2.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
                return title1.compareToIgnoreCase(title2);
            }
        });

        return tracks.iterator();
    }

    @Override
    public Iterable<String> categories() {
        return getCategories();
    }

    private MediaMetadataCompat buildFromJSON(JSONObject json) throws JSONException {
        String title = json.getString(JSON_TITLE);
        String album = json.getString(JSON_ALBUM);
        String genre = json.getString(JSON_GENRE);
        String source = json.getString(JSON_SOURCE);
        int duration = json.getInt(JSON_DURATION) * 1000; // ms

        String id = String.valueOf((genre+source).hashCode());

        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, source)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .build();
    }

    /**
     * Download a JSON file from a server, parse the content and return the JSON
     * object.
     *
     * @return result JSONObject containing the parsed representation.
     */
    private static JSONObject readJSON(){
        String jsonStr = readFromFile();
        try {
            return new JSONObject(jsonStr);
        } catch (JSONException e) {
            LogHelper.e(TAG, e, "Could not retrieve music list");
            return null;
        } catch (Exception e) {
            LogHelper.e(TAG, "Failed to parse the json for media list", e);
            return null;
        }
    }

    private static void writeJSON(JSONObject jsonObject) throws JSONException {
        String jsonStr = jsonObject.toString();
        writeToFile(jsonStr);
    }

    private static void writeToFile(String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(jsonFile));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private static String readFromFile() {

        String ret = "";

        try {
            InputStream inputStream = new FileInputStream(jsonFile);
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    public static ArrayList<String> getCategories(){
        ArrayList<String> categories = new ArrayList<>();
        try {
            JSONObject jsonObj = readJSON();
            if (jsonObj != null) {
                if(jsonObj.has(JSON_CATEGORIES)){
                    JSONArray jsonCategories = jsonObj.getJSONArray(JSON_CATEGORIES);
                    for (int i=0;i<jsonCategories.length();i++){
                        categories.add(jsonCategories.getString(i));
                    }
                }
            }
        } catch (JSONException e) {
            LogHelper.e(TAG, e, "Could not retrieve music list");
        }

        return categories;
    }

    public static void insertCategory(String category){
        JSONObject jsonObject = null;
        try {
            jsonObject = readJSON();
            if (jsonObject == null) {
                jsonObject = new JSONObject();
            }
            JSONArray categoriesArray = null;
            if(jsonObject.has(JSON_CATEGORIES)){
                categoriesArray = jsonObject.getJSONArray(JSON_CATEGORIES);
            }else{
                categoriesArray = new JSONArray();
            }
            categoriesArray.put(category);
            jsonObject.put(JSON_CATEGORIES, categoriesArray);
            writeJSON(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void insertMusic(String category, MediaMetadataCompat mediaMetadata){
        JSONObject jsonMediaObject = new JSONObject();
        try {
            jsonMediaObject.put(JSON_TITLE, mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            jsonMediaObject.put(JSON_ALBUM, mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));
            jsonMediaObject.put(JSON_GENRE, category);
            jsonMediaObject.put(JSON_SOURCE, mediaMetadata.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE));
            jsonMediaObject.put(JSON_DURATION, mediaMetadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)/1000);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject jsonObject = null;
        try {
            jsonObject = readJSON();
            if (jsonObject == null) {
                jsonObject = new JSONObject();
            }
            JSONArray musicArray = null;
            if(jsonObject.has(JSON_MUSIC)){
                musicArray = jsonObject.getJSONArray(JSON_MUSIC);
            }else{
                musicArray = new JSONArray();
            }
            musicArray.put(jsonMediaObject);
            jsonObject.put(JSON_MUSIC, musicArray);
            writeJSON(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String musicId = mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);

        if(!MusicProvider.mMusicListById.containsKey(musicId)){
            MusicProvider.mMusicListById.put(musicId, new MutableMediaMetadata(musicId, mediaMetadata));
            List<MediaMetadataCompat> list = MusicProvider.mMusicListByGenre.get(category);
            if (list == null) {
                list = new ArrayList<>();
                MusicProvider.mMusicListByGenre.put(category, list);
            }
            list.add(0, mediaMetadata);
        }
    }

    public static void deleteMusic(MediaMetadataCompat mediaMetadata){
        JSONObject jsonObject = null;
        try {
            jsonObject = readJSON();
            if (jsonObject == null) {
                jsonObject = new JSONObject();
            }
            JSONArray musicArray = null;
            if(jsonObject.has(JSON_MUSIC)){
                musicArray = jsonObject.getJSONArray(JSON_MUSIC);
            }else{
                musicArray = new JSONArray();
            }

            JSONArray newMusicList = new JSONArray();
            int len = musicArray.length();
            for (int i=0;i<len;i++)
            {
                JSONObject temp = musicArray.getJSONObject(i);
                String tempSource = temp.getString(JSON_SOURCE);
                String deletedSource = mediaMetadata.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
                //Excluding the item at position
                if (!tempSource.equals(deletedSource))
                {
                    newMusicList.put(musicArray.get(i));
                }
            }

            jsonObject.put(JSON_MUSIC, newMusicList);
            writeJSON(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
