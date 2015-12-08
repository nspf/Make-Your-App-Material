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

package com.example.xyzreader.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.activity.ArticleListActivity;
import com.example.xyzreader.ui.widget.ImageViewKeepRatio;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;

public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ViewHolder> {

    private final Cursor mCursor;
    private final Context mContext;
    private static final String EXTRA_CURRENT_ITEM_POSITION = "extra_current_item_position";


    public ArticleListAdapter(Context context,Cursor cursor) {
        mCursor = cursor;
        mContext = context;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        final ViewHolder vh;

        View mView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_article, parent, false);
        vh = new ViewHolder(mView);

        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ArticleListActivity) mContext).isReentering(false);
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
                intent.putExtra(EXTRA_CURRENT_ITEM_POSITION, vh.getAdapterPosition());
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation((Activity) mContext,
                                vh.thumbnailView, ArticleListActivity.SHARED_TRANSITION_IMAGE + vh.getAdapterPosition());
                mContext.startActivity(intent, options.toBundle());
                ((Activity) mContext).overridePendingTransition(0, 0);

            }
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        mCursor.moveToPosition(position);

        holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

        String article_meta = String.format(holder.articleMeta,
                DateUtils.getRelativeTimeSpanString(
                        mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString(),
                mCursor.getString(ArticleLoader.Query.AUTHOR));

        holder.subtitleView.setText(article_meta);


        Picasso.with(mContext)
                .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                .fit()
                .centerCrop()
                .into(holder.thumbnailView);

        ViewCompat.setTransitionName(holder.thumbnailView, ArticleListActivity.SHARED_TRANSITION_IMAGE + position);
        holder.thumbnailView.setTag(ArticleListActivity.SHARED_TRANSITION_IMAGE + position);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    public final class ViewHolder extends RecyclerView.ViewHolder  {

        @Bind(R.id.backdrop)                ImageViewKeepRatio thumbnailView;
        @Bind(R.id.article_title)           TextView titleView;
        @Bind(R.id.article_subtitle)        TextView subtitleView;
        @BindString(R.string.article_meta)          String articleMeta;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

}