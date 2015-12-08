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


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.fragment.ArticleDetailFragment;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.IntentPickerSheetView;
import com.xgc1986.parallaxPagerTransformer.ParallaxPagerTransformer;

import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.example.xyzreader.ui.activity.ArticleListActivity.EXTRA_CURRENT_ITEM_POSITION;
import static com.example.xyzreader.ui.activity.ArticleListActivity.EXTRA_OLD_ITEM_POSITION;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor mCursor;
    private MyPagerAdapter mPagerAdapter;
    private int mCurrentPosition;
    private int mOriginalPosition;
    private int mTopInset;
    private long mStartId;
    private boolean mIsReturning;

    private static final String STATE_CURRENT_POSITION = "state_current_position";
    private static final String STATE_OLD_POSITION = "state_old_position";

    @Bind(R.id.pager) ViewPager mPager;
    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.bottomsheet) BottomSheetLayout bottomSheet;
    @Bind(R.id.toolbar_container) AppBarLayout mToolbarContainer;
    @Bind(R.id.share_fab) FloatingActionButton mShareFab;

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {

            if (mIsReturning) {
                View sharedView = mPagerAdapter.getCurrentDetailsFragment().getSharedElement();
                if (sharedView == null) {
                    names.clear();
                    sharedElements.clear();
                } else if (mCurrentPosition != mOriginalPosition) {
                    names.clear();
                    sharedElements.clear();
                    names.add(ViewCompat.getTransitionName(sharedView));
                    sharedElements.put(ViewCompat.getTransitionName(sharedView), sharedView);
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);
        ButterKnife.bind(this);

        ActivityCompat.postponeEnterTransition(this);
        ActivityCompat.setEnterSharedElementCallback(this, mCallback);

        getSupportLoaderManager().initLoader(0, null, this);

        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            ActionBar mActionBar = getSupportActionBar();
            if (mActionBar != null){
                mActionBar.setDisplayHomeAsUpEnabled(true);
                mActionBar.setHomeButtonEnabled(true);
                mActionBar.setDisplayShowTitleEnabled(false);
            }
        }

        ViewCompat.animate(mShareFab)
                .setInterpolator(new OvershootInterpolator())
                .setStartDelay(500)
                .scaleX(1)
                .scaleY(1)
                .start();

        mToolbarContainer.getBackground().setAlpha(0);
        ViewCompat.setElevation(mToolbarContainer, 0);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            ViewCompat.setOnApplyWindowInsetsListener(mToolbarContainer, new OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    ViewCompat.onApplyWindowInsets(v,insets);
                    mTopInset = insets.getSystemWindowInsetTop();
                    mToolbarContainer.setPadding(0, mTopInset, 0, 0);
                    return insets;
                }
            });

            getWindow().setStatusBarColor(Color.TRANSPARENT);

        }

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mPager.setPageTransformer(false, new ParallaxPagerTransformer(R.id.backdrop));
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mCurrentPosition = position;
            }
        });


        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mOriginalPosition = mCurrentPosition;
            }

        }
        else {
            mCurrentPosition = savedInstanceState.getInt(STATE_CURRENT_POSITION);
            mOriginalPosition = savedInstanceState.getInt(STATE_OLD_POSITION);
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    mCurrentPosition = position;
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {

        private ArticleDetailFragment mCurrentFragment;

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentFragment = (ArticleDetailFragment) object;
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID),position);
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }

        public ArticleDetailFragment getCurrentDetailsFragment() {
            return mCurrentFragment;
        }

    }

    @SuppressWarnings("unused")
    @OnClick(R.id.share_fab)
    void onFabClick() {

        final Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setText("http://www.google.com")
                .setSubject("test")
                .setType("text/plain")
                .getIntent();

        IntentPickerSheetView intentPickerSheet = new IntentPickerSheetView(
                ArticleDetailActivity.this, shareIntent, "Share with...", new IntentPickerSheetView.OnIntentPickedListener() {
            @Override
            public void onIntentPicked(IntentPickerSheetView.ActivityInfo activityInfo) {
                bottomSheet.dismissSheet();
                startActivity(activityInfo.getConcreteIntent(shareIntent));
            }
        });

        bottomSheet.showWithSheetView(intentPickerSheet);

    }


    @Override
    public void finishAfterTransition() {
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(EXTRA_OLD_ITEM_POSITION, getIntent().getExtras().getInt(EXTRA_CURRENT_ITEM_POSITION));
        data.putExtra(EXTRA_CURRENT_ITEM_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_POSITION, mCurrentPosition);
        outState.putInt(STATE_OLD_POSITION, mOriginalPosition);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

}