package com.lifekau.android.lifekau.model;

import java.util.Date;

/**
 * Created by sgc109 on 2018-01-28.
 */

public class FoodReview extends Review {
    public int mCornerType;
    public FoodReview(){}
    public FoodReview(int cornerType, float rating, String comment){
        mCornerType = cornerType;
        mComment = comment;
        mRating = rating;
        mDate = new Date().getTime();
    }
}
