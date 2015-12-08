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

package com.example.xyzreader.ui.fragment;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.activity.ArticleDetailActivity;
import com.example.xyzreader.ui.activity.ArticleListActivity;
import com.example.xyzreader.ui.widget.ImageViewKeepRatio;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.BindString;
import butterknife.ButterKnife;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_ITEM_ID = "item_id";
    private static final String TAG = "ArticleDetailFragment";
    private static final String POSITION = "position";
    private static final float PARALLAX_FACTOR = 1.75f;

    private View mRootView;
    private Cursor mCursor;
    private int mPosition;
    private long mItemId;

    @Bind(R.id.article_title) TextView titleView;
    @Bind(R.id.article_byline) TextView bylineView;
    @Bind(R.id.article_body) TextView bodyView;
    @Bind(R.id.backdrop) ImageViewKeepRatio mPhotoView;
    @Bind(R.id.content) NestedScrollView mContent;
    @Bind(R.id.title_container) LinearLayout mTitleContainer;
    @BindString(R.string.article_meta) String articleMeta;
    @BindColor(R.color.primary_dark) int mPrimaryDarkColor;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId, int position) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putInt(POSITION, position);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(POSITION)) {
            mPosition = getArguments().getInt(POSITION);
        }
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        ButterKnife.bind(this, mRootView);
        bindViews();
        return mRootView;
    }


    private void bindViews() {

        final AppBarLayout toolbar_container = (AppBarLayout) getActivity().findViewById(R.id.toolbar_container);
        final Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        final AppBarLayout.LayoutParams p = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();


        if (mRootView == null) {
            return;
        }

        bylineView.setMovementMethod(new LinkMovementMethod());

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            String article_meta = String.format(articleMeta,
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString(),
                    mCursor.getString(ArticleLoader.Query.AUTHOR));

            bylineView.setText(article_meta);

            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

            ViewCompat.setTransitionName(mPhotoView, ArticleListActivity.SHARED_TRANSITION_IMAGE + mPosition);

            Picasso.with(getActivity())
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .fit()
                    .centerCrop()
                    .into(mPhotoView, new Callback() {

                        @Override
                        public void onSuccess() {

                            Bitmap bitmap = ((BitmapDrawable) mPhotoView.getDrawable()).getBitmap();
                            new Palette.Builder(bitmap).generate(new Palette.PaletteAsyncListener() {

                                @Override
                                public void onGenerated(Palette palette) {

                                    Palette.Swatch vibrant = palette.getVibrantSwatch();
                                    if (vibrant != null) {
                                        mTitleContainer.setBackgroundColor(vibrant.getRgb());
                                        titleView.setTextColor(vibrant.getTitleTextColor());
                                        bylineView.setTextColor(vibrant.getBodyTextColor());

                                    } else {
                                        mTitleContainer.setBackgroundColor(mPrimaryDarkColor);
                                    }

                                    titleView.setTextColor(Color.WHITE);
                                    bylineView.setTextColor(Color.WHITE);

                                }
                            });
                        }

                        @Override
                        public void onError() {

                        }
                    });

            mContent.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
                @Override
                public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

                    /**
                     * Do parallax scrolling for backdrop image
                     */
                    mPhotoView.setTranslationY(scrollY / PARALLAX_FACTOR);


                    /**
                     * Fade status bar background color on scroll
                     */
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        float alpha = Math.min(1, (float) (scrollY) / mPhotoView.getHeight());
                        getActivity().getWindow().setStatusBarColor(getColorWithAlpha(alpha, mPrimaryDarkColor));
                    }


                    if (scrollY < (mPhotoView.getHeight() - getStatusBarHeight())) {
                        p.setScrollFlags(0);
                        toolbar.setLayoutParams(p);
                        ViewCompat.setElevation(toolbar_container, 0);
                        toolbar_container.getBackground().setAlpha(0);

                    } else {
                        p.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                                                | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
                        toolbar.setLayoutParams(p);
                        ViewCompat.setElevation(toolbar_container, toolbar_container.getTargetElevation());
                        toolbar_container.getBackground().setAlpha(255);
                    }
                }
            });


        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }


        bindViews();

        mRootView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRootView.getViewTreeObserver().removeOnPreDrawListener(this);
                ActivityCompat.startPostponedEnterTransition(getActivity());
                return true;
            }
        });

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    /**
     * Returns the shared element that should be transitioned back to the previous Activity,
     * or null if the view is not visible on screen.
     */
    @Nullable
    public View getSharedElement() {
        if (isViewInBounds(mContent, mPhotoView)) {
            return mPhotoView;
        }
        return null;
    }

    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }


    private static int getColorWithAlpha(float alpha, int baseColor) {
        int a = Math.min(255, Math.max(0, (int) (alpha * 255))) << 24;
        int rgb = 0x00ffffff & baseColor;
        return a + rgb;
    }


    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

}
