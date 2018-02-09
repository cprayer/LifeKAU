package com.lifekau.android.lifekau.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lifekau.android.lifekau.R;
import com.lifekau.android.lifekau.model.FoodReview;

import java.util.ArrayList;
import java.util.List;

public class FoodReviewCornerListActivity extends AppCompatActivity {
    private int mRestaurantType;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mRecyclerAdapter;
    private List<Integer> mListCntReviews;
    private List<Float> mListSumReviewRatings;
    private ProgressBar mProgressBar;
    private boolean mIsWriting;

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, FoodReviewCornerListActivity.class);
        return intent;
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_review_corner_list);


        if(getSupportActionBar() != null ) {
//            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().hide();
        }
        int cntCorners = getResources().getStringArray(R.array.food_corner_list).length;
        if (mListCntReviews == null) {
            mListCntReviews = new ArrayList<>();
            for(int i = 0; i < cntCorners; i++) mListCntReviews.add(0);
        }
        if (mListSumReviewRatings == null) {
            mListSumReviewRatings = new ArrayList<>();
            for(int i = 0; i < cntCorners; i++) mListSumReviewRatings.add(0.0f);
        }

        mRecyclerAdapter = new RecyclerView.Adapter<FoodCornerViewHolder>() {
            @Override
            public FoodCornerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_food_review_corner, parent, false);
                return new FoodCornerViewHolder(view);
            }

            @Override
            public void onBindViewHolder(FoodCornerViewHolder holder, int position) {
                holder.bind(position);
            }

            @Override
            public int getItemCount() {
                return getResources().getStringArray(R.array.food_corner_list).length;
            }
        };
        mRecyclerView = (RecyclerView) findViewById(R.id.food_review_corner_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(mRecyclerAdapter);

        mProgressBar = findViewById(R.id.food_review_corner_list_progress_bar);
        mProgressBar.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/" + getString(R.string.firebase_database_food_reviews));
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot cornerSnapshot : dataSnapshot.getChildren()) {
                    int cornerType = -1;
                    Float sumRating = 0.0f;
                    int cntReviews = 0;
                    for (DataSnapshot reviewSnapshot : cornerSnapshot.getChildren()) {
                        cntReviews++;
                        FoodReview review = reviewSnapshot.getValue(FoodReview.class);
                        if(cornerType == -1) cornerType = review.mCornerType;
                        sumRating += review.mRating;
                    }
                    mListCntReviews.set(cornerType, cntReviews);
                    mListSumReviewRatings.set(cornerType, sumRating);
                }
                mRecyclerAdapter.notifyItemRangeChanged(0, getResources().getStringArray(R.array.food_corner_list).length);
                mProgressBar.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public class FoodCornerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private RatingBar mRatingBar;
        private TextView mCornerNameTextView;
        private TextView mAvgRatingTextView;
        private TextView mCntReviewTextView;
        private Context mContext;

        public FoodCornerViewHolder(View itemView) {
            super(itemView);
            mRatingBar = (RatingBar) itemView.findViewById(R.id.list_item_food_corner_rating_bar);
            mCornerNameTextView = (TextView) itemView.findViewById(R.id.list_item_food_corner_name_text_view);
            mAvgRatingTextView = (TextView) itemView.findViewById(R.id.list_item_food_corner_avg_rating);
            mCntReviewTextView = (TextView) itemView.findViewById(R.id.list_item_food_corner_cnt_reviews);
            mContext = itemView.getContext();
            itemView.setOnClickListener(this);
        }

        public void bind(int position) { // 구현하기
            int cntReviews = mListCntReviews.get(position);
            float avgRating = cntReviews > 0 ? mListSumReviewRatings.get(position) / cntReviews : 0;
            mRatingBar.setRating(avgRating);
            mCornerNameTextView.setText(getResources().getStringArray(R.array.food_corner_list)[position]);
            mAvgRatingTextView.setText("" + String.format("%.2f", avgRating) + "/ 5.0");
            mCntReviewTextView.setText("" + cntReviews + getString(R.string.review_number_of_review_string));
        }

        @Override
        public void onClick(View view) {
            Intent intent = FoodReviewListActivity.newIntent(mContext, this.getAdapterPosition());
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRecyclerAdapter != null) {
            mRecyclerAdapter.notifyItemRangeChanged(0, getResources().getStringArray(R.array.food_corner_list).length);
        }
    }
}
