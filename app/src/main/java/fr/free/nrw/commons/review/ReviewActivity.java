package fr.free.nrw.commons.review;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.material.navigation.NavigationView;
import com.viewpagerindicator.CirclePageIndicator;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.mwapi.MediaResult;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.MediaDataExtractorUtil;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ReviewActivity extends AuthenticatedActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.navigation_view)
    NavigationView navigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @BindView(R.id.reviewPager)
    ReviewViewPager reviewPager;

    @BindView(R.id.skip_image)
    Button skip_image_button;

    @Inject MediaWikiApi mwApi;

    public ReviewPagerAdapter reviewPagerAdapter;

    public ReviewController reviewController;

    @BindView(R.id.reviewPagerIndicator)
    public CirclePageIndicator pagerIndicator;


    @Override
    protected void onAuthCookieAcquired(String authCookie) {

    }

    @Override
    protected void onAuthFailure() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        ButterKnife.bind(this);
        initDrawer();

        reviewController = new ReviewController(this);

        reviewPagerAdapter = new ReviewPagerAdapter(getSupportFragmentManager());
        reviewPager.setAdapter(reviewPagerAdapter);
        reviewPagerAdapter.getItem(0);
        pagerIndicator.setViewPager(reviewPager);

        runRandomizer(); //Run randomizer whenever everything is ready so that a first random image will be added

        skip_image_button.setOnClickListener(view -> runRandomizer());
    }



    public boolean runRandomizer() {
        ProgressBar progressBar = reviewPagerAdapter.reviewImageFragments[reviewPager.getCurrentItem()].progressBar;
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        reviewPager.setCurrentItem(0);
        Observable.fromCallable(() -> {
            String result = "";
            try {
                Media media = mwApi.getRecentRandomImage();
                if (media != null) {
                    result = media.getFilename();
                }
            } catch (IOException e) {
                Timber.e("Error fetching recent random image: " + e.toString());
            }
            return result;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateImage);
        return true;
    }

    private void updateImage(String fileName) {
        if (fileName.length() == 0) {
            ViewUtil.showShortSnackbar(drawerLayout, R.string.error_review);
            return;
        }
        reviewController.onImageRefreshed(fileName); //file name is updated
        mwApi.firstRevisionOfFile("File:" + fileName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(revision -> {
                    reviewController.firstRevision = revision;
                    reviewPagerAdapter.updateFileInformation(fileName, revision);
                });
        reviewPager.setCurrentItem(0);
        Observable.fromCallable(() -> {
            MediaResult media = mwApi.fetchMediaByFilename("File:" + fileName);
            return MediaDataExtractorUtil.extractCategories(media.getWikiSource());
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateCategories, this::categoryFetchError);


    }

    private void categoryFetchError(Throwable throwable) {
        Timber.e(throwable, "Error fetching categories");
        ViewUtil.showShortSnackbar(drawerLayout, R.string.error_review_categories);
    }

    private void updateCategories(ArrayList<String> categories) {
        reviewController.onCategoriesRefreshed(categories);
        reviewPagerAdapter.updateCategories();
    }

    /**
     * References ReviewPagerAdapter to null before the activity is destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Consumers should be simply using this method to use this activity.
     * @param context
     * @param title Page title
     */
    public static void startYourself(Context context, String title) {
        Intent reviewActivity = new Intent(context, ReviewActivity.class);
        context.startActivity(reviewActivity);
    }
}
