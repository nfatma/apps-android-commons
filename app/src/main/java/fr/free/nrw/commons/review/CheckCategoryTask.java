package fr.free.nrw.commons.review;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.view.Gravity;
import android.widget.Toast;

import javax.inject.Inject;

import androidx.core.app.NotificationCompat;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;


// Example code:
// CheckCategoryTask deleteTask = new CheckCategoryTask(getActivity(), media);

// TODO: refactor; see DeleteTask and SendThankTask
public class CheckCategoryTask extends AsyncTask<Void, Integer, Boolean> {

    @Inject
    MediaWikiApi mwApi;
    @Inject
    SessionManager sessionManager;

    public static final int NOTIFICATION_CHECK_CATEGORY = 0x101;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Context context;
    private Media media;

    public CheckCategoryTask(Context context, Media media){
        this.context = context;
        this.media = media;
    }

    @Override
    protected void onPreExecute(){
        ApplicationlessInjection
                .getInstance(context.getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context);
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER,0,0);
        toast = Toast.makeText(context, context.getString(R.string.check_category_toast, media.getDisplayTitle()), Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    protected Boolean doInBackground(Void ...voids) {
        publishProgress(0);

        String editToken;
        String authCookie;
        String summary = context.getString(R.string.check_category_edit_summary);

        authCookie = sessionManager.getAuthCookie();
        mwApi.setAuthCookie(authCookie);

        try {
            editToken = mwApi.getEditToken();
            if (editToken.equals("+\\")) {
                return false;
            }
            publishProgress(1);

            mwApi.appendEdit(editToken, "\n{{subst:chc}}\n", media.getFilename(), summary);
            publishProgress(2);
        }
        catch (Exception e) {
            Timber.d(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    protected void onProgressUpdate (Integer... values){
        super.onProgressUpdate(values);

        int[] messages = new int[]{R.string.getting_edit_token, R.string.check_category_adding_template};
        String message = "";
        if (0 < values[0] && values[0] < messages.length) {
            message = context.getString(messages[values[0]]);
        }

        notificationBuilder.setContentTitle(context.getString(R.string.check_category_notification_title, media.getDisplayTitle()))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(messages.length, values[0], false)
                .setOngoing(true);
        notificationManager.notify(NOTIFICATION_CHECK_CATEGORY, notificationBuilder.build());
    }

    @Override
    protected void onPostExecute(Boolean result) {
        String message = "";
        String title = "";

        if (result){
            title = context.getString(R.string.check_category_success_title);
            message = context.getString(R.string.check_category_success_message, media.getDisplayTitle());
        }
        else {
            title = context.getString(R.string.check_category_failure_title);
            message = context.getString(R.string.check_category_failure_message, media.getDisplayTitle());
        }

        notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(0,0,false)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationManager.notify(NOTIFICATION_CHECK_CATEGORY, notificationBuilder.build());
    }
}