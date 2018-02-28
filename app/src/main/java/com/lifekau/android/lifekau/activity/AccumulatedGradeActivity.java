package com.lifekau.android.lifekau.activity;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lifekau.android.lifekau.R;
import com.lifekau.android.lifekau.manager.LMSPortalManager;
import com.lifekau.android.lifekau.model.AccumulatedGrade;
import com.lifekau.android.lifekau.model.Scholarship;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.Calendar;

public class AccumulatedGradeActivity extends AppCompatActivity {

    private static final String EXTRA_YEAR = "extra_year";
    private static final String EXTRA_SEMESTER_CODE = "extra_semester_code";

    private static final int DEFAULT_SEMESTER_CODE = 10;

    private static final int UNEXPECTED_ERROR = -100;
    private static final int MAXIMUM_RETRY_NUM = 5;

    private LMSPortalManager mLMSPortalManager = LMSPortalManager.getInstance();
    private RecyclerView.Adapter mRecyclerAdapter;
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private GetAccumulatedGradeAsyncTask mGetAccumulatedGradeAsyncTask;
    private int mYear;
    private int mSemesterCode;

    public static Intent newIntent(Context context, int year, int semesterCode) {
        Intent intent = new Intent(context, AccumulatedGradeActivity.class);
        intent.putExtra(EXTRA_YEAR, year);
        intent.putExtra(EXTRA_SEMESTER_CODE, semesterCode);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grade);
        Intent intent = getIntent();
        Calendar now = Calendar.getInstance();
        mYear = intent.getIntExtra(EXTRA_YEAR, now.get(Calendar.YEAR));
        mSemesterCode = intent.getIntExtra(EXTRA_SEMESTER_CODE, DEFAULT_SEMESTER_CODE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        mRecyclerAdapter = new RecyclerView.Adapter<AccumulatedGradeItemViewHolder>() {
            @Override
            public AccumulatedGradeItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_grade, parent, false);
                return new AccumulatedGradeItemViewHolder(view);
            }

            @Override
            public void onBindViewHolder(AccumulatedGradeItemViewHolder holder, int position) {
                holder.bind(position);
            }

            @Override
            public int getItemCount() {
                return mLMSPortalManager.getAccumulatedGradeSize();
            }
        };

        mLMSPortalManager.clearAccumulatedGrade();
        mRecyclerView = findViewById(R.id.grade_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(mRecyclerAdapter);
//        mRecyclerView.setVisibility(View.GONE);
        mProgressBar = findViewById(R.id.grade_list_progress_bar);
        mProgressBar.setVisibility(View.GONE);
        executeAsyncTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRecyclerAdapter != null) {
            mRecyclerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGetAccumulatedGradeAsyncTask.cancel(true);
    }

    public void executeAsyncTask() {
        mGetAccumulatedGradeAsyncTask = new GetAccumulatedGradeAsyncTask(getApplication(), this);
        mGetAccumulatedGradeAsyncTask.execute();
    }

    public class AccumulatedGradeItemViewHolder extends RecyclerView.ViewHolder {
        private TextView mSubjectTitleTextView;
        private TextView mProfessorNameTextView;
        private TextView mTypeTextView;
        private TextView mCreditsTextView;
        private TextView mGradeTextView;

        public AccumulatedGradeItemViewHolder(View itemView) {
            super(itemView);
            mSubjectTitleTextView = itemView.findViewById(R.id.list_item_grade_subject_title);
            mProfessorNameTextView = itemView.findViewById(R.id.list_item_grade_professor_name);
            mTypeTextView = itemView.findViewById(R.id.list_item_grade_type);
            mCreditsTextView = itemView.findViewById(R.id.list_item_grade_credits);
            mGradeTextView = itemView.findViewById(R.id.list_item_grade_grade);
        }

        public void bind(int position) {
            AccumulatedGrade accumulatedGrade = mLMSPortalManager.getAccumulatedGrade(position);
            mSubjectTitleTextView.setText(accumulatedGrade.subjectTitle);
            mProfessorNameTextView.setText(accumulatedGrade.professorName);
            mTypeTextView.setText(accumulatedGrade.type);
            mCreditsTextView.setText(String.valueOf(accumulatedGrade.credits));
            mGradeTextView.setText(accumulatedGrade.grade);
        }
    }

    private static class GetAccumulatedGradeAsyncTask extends AsyncTask<Void, Void, Integer> {

        private WeakReference<AccumulatedGradeActivity> activityReference;
        private WeakReference<Application> applicationWeakReference;

        GetAccumulatedGradeAsyncTask(Application application, AccumulatedGradeActivity AccumulatedGradeActivity) {
            applicationWeakReference = new WeakReference<>(application);
            activityReference = new WeakReference<>(AccumulatedGradeActivity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            AccumulatedGradeActivity accumulatedGradeActivity = activityReference.get();
            if (accumulatedGradeActivity == null || accumulatedGradeActivity.isFinishing()) return;
            accumulatedGradeActivity.mLMSPortalManager.clearScholarship();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            AccumulatedGradeActivity accumulatedGradeActivity = activityReference.get();
            if (accumulatedGradeActivity == null || accumulatedGradeActivity.isFinishing()) return UNEXPECTED_ERROR;
            Resources resources = accumulatedGradeActivity.getResources();
            int count = 0;
            int year = accumulatedGradeActivity.mYear;
            int semesterCode = accumulatedGradeActivity.mSemesterCode;
            int result = accumulatedGradeActivity.mLMSPortalManager.pullAccumulatedGrade(activityReference.get(), year, semesterCode);
            while (!accumulatedGradeActivity.isFinishing() && result != resources.getInteger(R.integer.no_error) && !isCancelled()) {
                sleep(3000);
                if(result == resources.getInteger(R.integer.session_error)) return result;
                else count++;
                if(count == MAXIMUM_RETRY_NUM) return resources.getInteger(R.integer.network_error);
            }
            return resources.getInteger(R.integer.no_error);
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            AccumulatedGradeActivity accumulatedGradeActivity = activityReference.get();
            if (accumulatedGradeActivity == null || accumulatedGradeActivity.isFinishing()) return;
            Resources resources = accumulatedGradeActivity.getResources();
            if (result == resources.getInteger(R.integer.no_error)) {
//                accumulatedGradeActivity.mProgressBar.setVisibility(View.GONE);
//                accumulatedGradeActivity.mRecyclerView.setVisibility(View.VISIBLE);
                accumulatedGradeActivity.mRecyclerAdapter.notifyDataSetChanged();
            } else if(result == resources.getInteger(R.integer.network_error)){
                //네트워크 관련 문제
                accumulatedGradeActivity.showErrorMessage();
            }
            else if(result == resources.getInteger(R.integer.session_error)){
                //세션 관련 문제
                Intent intent = LoginActivity.newIntent(accumulatedGradeActivity);
                accumulatedGradeActivity.startActivity(intent);
                accumulatedGradeActivity.finish();
            }
        }

        public void sleep(int time) {
            try {
                Thread.sleep(time);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void showErrorMessage() {
        Toast toast = Toast.makeText(getApplicationContext(), "오류가 발생하였습니다.", Toast.LENGTH_SHORT);
        toast.show();
    }
}