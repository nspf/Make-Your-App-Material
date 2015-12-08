/*
 * Copyright 2015 Nicolas Pintos
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

package com.example.xyzreader.data;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

import com.example.xyzreader.remote.Article;
import com.example.xyzreader.remote.RestClient;

import java.util.ArrayList;
import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class UpdaterService extends IntentService {
    private static final String TAG = "UpdaterService";

    public static final String BROADCAST_ACTION_STATE_CHANGE
            = "com.example.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_REFRESHING
            = "com.example.xyzreader.intent.extra.REFRESHING";
    public static final String BROADCAST_ACTION_ERROR_NETWORK
            = "com.example.xyzreader.intent.action.ERROR_NETWORK";


    public UpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Time time = new Time();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            sendBroadcast(new Intent(BROADCAST_ACTION_ERROR_NETWORK));
            return;
        }

        sendBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, true));

        Call<List<Article>> articleList = RestClient.build().getArticleList();
        articleList.enqueue(new Callback<List<Article>>() {
            @Override
            public void onResponse(Response<List<Article>> response, Retrofit retrofit) {

                ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
                Uri dirUri = ItemsContract.Items.buildDirUri();

                // Delete all items
                cpo.add(ContentProviderOperation.newDelete(dirUri).build());

                for (int i = 0; i < response.body().size(); i++) {
                    ContentValues values = new ContentValues();
                    Article article = response.body().get(i);
                    values.put(ItemsContract.Items.SERVER_ID, article.getId());
                    values.put(ItemsContract.Items.AUTHOR, article.getAuthor());
                    values.put(ItemsContract.Items.TITLE, article.getTitle());
                    values.put(ItemsContract.Items.BODY, article.getBody());
                    values.put(ItemsContract.Items.THUMB_URL, article.getThumb());
                    values.put(ItemsContract.Items.PHOTO_URL, article.getPhoto());
                    values.put(ItemsContract.Items.ASPECT_RATIO, article.getAspectRatio());
                    time.parse3339(article.getPublishedDate());
                    values.put(ItemsContract.Items.PUBLISHED_DATE, time.toMillis(false));
                    cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
                }

                try {
                    getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo);

                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(TAG, "Error updating content.", e);
                }

            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });

        sendBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, false));
    }
}
