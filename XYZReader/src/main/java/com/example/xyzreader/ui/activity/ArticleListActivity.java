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

package com.example.xyzreader.ui.activity;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewTreeObserver;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.ui.adapter.ArticleListAdapter;

import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.BindInt;
import butterknife.BindString;
import butterknife.ButterKnife;
import jp.wasabeef.recyclerview.animators.adapters.AlphaInAnimationAdapter;
import jp.wasabeef.recyclerview.animators.adapters.SlideInBottomAnimationAdapter;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    @Bind(R.id.main_content) CoordinatorLayout mMainContainer;
    @Bind(R.id.recycler_view) RecyclerView mRecyclerView;
    @Bind(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;
    @Bind(R.id.toolbar) Toolbar mToolbar;
    @BindString(R.string.error_network) String mNetworkNerror;
    @BindString(R.string.retry) String mRetry;
    @BindInt(R.integer.list_column_count) int mListColumnCount;

    private Snackbar mSnackbar;
    private Bundle mTmpState;
    private boolean mIsReentering;
    private boolean mIsRefreshing = false;

    static final String EXTRA_CURRENT_ITEM_POSITION = "extra_current_item_position";
    static final String EXTRA_OLD_ITEM_POSITION = "extra_old_item_position";
    public static final String SHARED_TRANSITION_IMAGE = "image";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_ERROR_NETWORK));
    }


    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private final BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {

                case UpdaterService.BROADCAST_ACTION_STATE_CHANGE:
                    mIsRefreshing = intent.getExtras().getBoolean(UpdaterService.EXTRA_REFRESHING, false);
                    updateRefreshingUI();
                    break;

                case UpdaterService.BROADCAST_ACTION_ERROR_NETWORK:
                    mSnackbar = Snackbar.make(mMainContainer, mNetworkNerror, Snackbar.LENGTH_INDEFINITE);
                    mSnackbar.setAction(mRetry, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            refresh();
                            mSnackbar.dismiss();
                        }
                    });
                    mSnackbar.show();
                    break;
            }

        }
    };

    private void updateRefreshingUI() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
            }
        }, 2000);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        final ArticleListAdapter adapter = new ArticleListAdapter(this,cursor);
        adapter.setHasStableIds(true);
        AlphaInAnimationAdapter alphaAdapter = new AlphaInAnimationAdapter(adapter);

        GridLayoutManager layoutManager = new GridLayoutManager(this, mListColumnCount);

        mRecyclerView.setAdapter(new SlideInBottomAnimationAdapter(alphaAdapter));
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        setExitSharedElementCallback(mCallback);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }


    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);
        mIsReentering = true;
        mTmpState = new Bundle(data.getExtras());
        int oldPosition = mTmpState.getInt(EXTRA_OLD_ITEM_POSITION);
        int currentPosition = mTmpState.getInt(EXTRA_CURRENT_ITEM_POSITION);
        if (oldPosition != currentPosition) {
            mRecyclerView.scrollToPosition(currentPosition);
        }
        ActivityCompat.postponeEnterTransition(this);
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                mRecyclerView.requestLayout();
                ActivityCompat.startPostponedEnterTransition(ArticleListActivity.this);
                return true;
            }
        });
    }


    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mIsReentering) {
                int oldPosition = mTmpState.getInt(EXTRA_OLD_ITEM_POSITION);
                int currentPosition = mTmpState.getInt(EXTRA_CURRENT_ITEM_POSITION);

                if (currentPosition != oldPosition) {
                    String newTransitionName = SHARED_TRANSITION_IMAGE + currentPosition;
                    View newSharedView = mRecyclerView.findViewWithTag(newTransitionName);

                    if (newSharedView != null) {
                        names.clear();
                        names.add(newTransitionName);
                        sharedElements.clear();
                        sharedElements.put(newTransitionName, newSharedView);
                    }
                }
                mTmpState = null;
            }
        }
    };


    public void isReentering(boolean isReentering) {
        mIsReentering = isReentering;
    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

}
