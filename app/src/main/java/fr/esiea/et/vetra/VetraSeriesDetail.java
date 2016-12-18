package fr.esiea.et.vetra;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

/*************************************************************
 **															**
 **			____   ____      __                 			**
 **			\   \ /   /_____/  |_____________   			**
 **			 \   Y   // __ \   __\_  __ \__  \  			**
 **			  \     /\  ___/|  |  |  | \// __ \_			**
 **		 	   \___/  \___  >__|  |__|  (____  /			**
 **			              \/                 \/ 			**
 **															**
 **															**
 **************************************************************/

public class VetraSeriesDetail extends AppCompatActivity
{
    private VetraDataStructure data = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vetra_series_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        int ID = intent.getIntExtra(VetraListAdapter.TRANSACTIVITY_CONTAINER, -1);

        //Invalid idea, too bad...
        if(ID == -1)
        {
            close();
            return;
        }

        data = VetraCoreData.getDataWithID(ID);
        if(data == null || !data.containsFull)
        {
            //We need to defer the opening of the tab
            new DeferredOpening(this, intent);
            close();
            return;
        }

        this.setTitle(data.name);

        File file = new File(this.getCacheDir(), data.backdropURL);
        if(file.exists())
        {
            ((ImageView) findViewById(R.id.detail_image)).setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
        }

        ((TextView) findViewById(R.id.detail_creator)).setText(craftLine(getString(R.string.detail_createdby), data.creatorName));
        ((TextView) findViewById(R.id.detail_productor)).setText(craftLine(getString(R.string.detail_producedby), data.productorName));
        ((TextView) findViewById(R.id.detail_language)).setText(craftLine(getString(R.string.detail_language), new Locale(data.language).getDisplayLanguage()));
        ((TextView) findViewById(R.id.detail_network)).setText(craftLine(getString(R.string.detail_network), data.channel));
        ((TextView) findViewById(R.id.detail_synopsis)).setText(craftLine(getString(R.string.detail_synopsis), data.synopsis));

        String seasonInfo = "<html><b><u>" + getString(R.string.detail_seasons) + "</u></b> " + Integer.toString(data.numberSeason.intValue() + 1) + " – " +
                data.numberEpisodes.toString() + " " + getString(R.string.detail_episodes) + "</html>";
        ((TextView) findViewById(R.id.detail_seasonInfo)).setText(Html.fromHtml(seasonInfo, Html.FROM_HTML_MODE_COMPACT));

        RecyclerView list = (RecyclerView) findViewById(R.id.detail_list);

        list.setAdapter(new VetraSeriesDetailAdapter(this, data));
        list.setLayoutManager(new LinearLayoutManager(this));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ScrollView scrollView = (ScrollView) findViewById(R.id.detail_general_list);
                scrollView.smoothScrollTo(0, 0);
            }
        }, 100);
    }

    private Spanned craftLine(String base, String data)
    {
        return Html.fromHtml("<html><b><u>" + base + "</u></b>  " + data + "</html>", Html.FROM_HTML_MODE_COMPACT);
    }

    private void close()
    {
        finish();
        overridePendingTransition(R.animator.exit_1, R.animator.exit_2);
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        overridePendingTransition(R.animator.exit_1, R.animator.exit_2);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_details, menu);

        if(isFavs())
            menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_highlight_off_black_24dp));

        return true;
    }

    private boolean isFavs()
    {
        SQLiteDatabase database = VetraCoreData.getDatabase();
        int count = 0;

        if(database != null)
        {
            Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + VetraSqliteNames.TABLE_FAVORITES + " WHERE " + VetraSqliteNames.FAV_ID + " = " + data.ID.toString(), null);

            if(cursor.getCount() == 1)
            {
                cursor.moveToFirst();
                count = cursor.getInt(0);
            }

            cursor.close();
            database.close();
        }

        return count != 0;
    }

    //Menu button management
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_open)
        {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(data.homepageURL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("com.android.chrome");

            try
            {
                startActivity(intent);
            }
            catch (ActivityNotFoundException ex)
            {
                // Chrome browser presumably not installed so allow user to choose instead
                intent.setPackage(null);
                startActivity(intent);
            }

            return true;
        }
        else if (id == R.id.toggle_favs)
        {
            final boolean addToFavs = item.getIcon().getConstantState() == getResources().getDrawable(R.drawable.ic_favorite_black_24dp).getConstantState();

            if(addToFavs)
            {
                item.setIcon(getResources().getDrawable(R.drawable.ic_highlight_off_black_24dp));
                VetraCoreData.addToFavs(data.ID.intValue());
            }
            else
            {
                final MenuItem _item = item;
                new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.alert_removeFavs_title))
                        .setMessage(getResources().getString(R.string.alert_removeFavs_content))
                        .setPositiveButton(getResources().getString(R.string.alert_removeFavs_yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                //Confirmed we had to remove from favs
                                _item.setIcon(getResources().getDrawable(R.drawable.ic_favorite_black_24dp));
                                VetraCoreData.removeFromFavs(data.ID.intValue());
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.alert_removeFavs_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}})
                        .create().show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    //Deferred opening management
    private static class DeferredOpening extends BroadcastReceiver
    {
        Context context;
        Intent intent;

        public DeferredOpening(Context context, Intent _originalIntent)
        {
            intent = _originalIntent;
            LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(VetraCoreData.INTENT_DL_DETAILED_OVER));
        }

        @Override
        public void onReceive(Context context, Intent _intent)
        {
            context.startActivity(intent);
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        }
    }
}
