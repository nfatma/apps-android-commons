package fr.free.nrw.commons.review;

import android.content.Context;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.delete.DeleteTask;
import fr.free.nrw.commons.mwapi.Revision;

public class ReviewController {
    private String fileName;
    @Nullable
    public Revision firstRevision; // TODO: maybe we can expand this class to include fileName
    protected static ArrayList<String> categories;

    private ReviewPagerAdapter reviewPagerAdapter;
    private ViewPager viewPager;
    private ReviewActivity reviewActivity;

    ReviewController(Context context) {
        reviewActivity =  (ReviewActivity)context;
        reviewPagerAdapter = reviewActivity.reviewPagerAdapter;
        viewPager = ((ReviewActivity)context).reviewPager;
    }

    public void onImageRefreshed(String fileName) {
        this.fileName = fileName;
        ReviewController.categories = new ArrayList<>();
    }

    public void onCategoriesRefreshed(ArrayList<String> categories) {
        ReviewController.categories = categories;
    }

    public void swipeToNext() {
        int nextPos = viewPager.getCurrentItem()+1;
        if (nextPos <= 3) {
            viewPager.setCurrentItem(nextPos);
        } else {
            reviewActivity.runRandomizer();
        }
    }

    public void reportSpam() {
        DeleteTask.askReasonAndExecute(new Media("File:"+fileName),
                reviewActivity,
                reviewActivity.getResources().getString(R.string.review_spam_report_question),
                reviewActivity.getResources().getString(R.string.review_spam_report_problem));
    }

    public void reportPossibleCopyRightViolation() {
        DeleteTask.askReasonAndExecute(new Media("File:"+fileName),
                reviewActivity,
                reviewActivity.getResources().getString(R.string.review_c_violation_report_question),
                reviewActivity.getResources().getString(R.string.review_c_violation_report_problem));
    }

    public void reportWrongCategory() {
        new CheckCategoryTask(reviewActivity, new Media("File:"+fileName)).execute();
        swipeToNext();
    }

    public void sendThanks() {
        new SendThankTask(reviewActivity, new Media("File:"+fileName), firstRevision).execute();
        swipeToNext();
    }
}
