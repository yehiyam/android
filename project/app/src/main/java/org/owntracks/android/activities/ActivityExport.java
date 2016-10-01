package org.owntracks.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Parser;

public class ActivityExport extends ActivityBase {
    private static final String TAG = "ActivityExport";

    private static final String TEMP_FILE_NAME = "config.otrc";
    private static Context context;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_export);

        setSupportToolbar();
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new FragmentPreferencesExport(), "exportOreferences").commit();

    }

    public static class FragmentPreferencesExport extends PreferenceFragment {


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Activity a = getActivity();

            addPreferencesFromResource(R.xml.export);

            findPreference("exportToFile").setOnPreferenceClickListener(exportToFile);
            findPreference("exportWaypointsToEndpoint").setOnPreferenceClickListener(exportWaypointsToEndpoint);
            findPreference("import").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Intent intent = new Intent(context, ActivityImport.class);
                    intent.putExtra(ActivityImport.FLAG_IN_APP, true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    intent.putExtra(ActivityBase.DISABLES_ANIMATION, true);
                    startActivity(intent);
                    return true;
                }
            });

        }

    }



    private static Preference.OnPreferenceClickListener exportToFile = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            String exportStr;
            try {
                exportStr = Parser.toJsonPlain(Preferences.exportToMessage());
            } catch (IOException e) {
                Toast.makeText(context, R.string.preferencesExportFailed, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return false;
            }


            if(exportStr == null)
                return false;


            Log.v("Export", "Config: \n" + exportStr);
            File cDir = App.getInstance().getBaseContext().getCacheDir();
            File tempFile = new File(cDir.getPath() + "/" + TEMP_FILE_NAME) ;

            try {
                FileWriter writer = new FileWriter(tempFile);

                writer.write(exportStr);
                writer.close();

                Log.v(TAG, "Saved temporary config file for exportToMessage to " + tempFile.getPath());

            } catch (IOException e) {
                e.printStackTrace();
            }
            Uri configUri = FileProvider.getUriForFile(App.getInstance(), "org.owntracks.android.fileprovider", tempFile);

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, configUri);
            sendIntent.setType("text/plain");

            context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.exportConfiguration)));
            Toast.makeText(context, R.string.preferencesExportSuccess, Toast.LENGTH_SHORT).show();

            return false;
        }
    };

    private static Preference.OnPreferenceClickListener exportWaypointsToEndpoint = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {

            ServiceProxy.runOrBind(context, new Runnable() {
                @Override
                public void run() {
                    if(ServiceProxy.getServiceLocator().publishWaypointsMessage()) {
                        Toast.makeText(context, R.string.preferencesExportQueued, Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(context, R.string.preferencesExportFailed, Toast.LENGTH_SHORT).show();

                    }
                }
            });
            return false;
        }
    };


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


}
