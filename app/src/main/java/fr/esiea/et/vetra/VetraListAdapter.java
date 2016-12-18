package fr.esiea.et.vetra;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.text.ParseException;
import java.util.Date;

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

public class VetraListAdapter extends RecyclerView.Adapter
{

    public final static String TRANSACTIVITY_CONTAINER = "fr.esiea.et.vetra.detailPayload";

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        final TextView mTextView;
        private final ImageView imageView;
        private ViewHolderBroadcastReceiver receiver;

        public static class ViewHolderBroadcastReceiver extends BroadcastReceiver
        {
            private boolean hasRegistered;
            private String posterURL = null;
            private final ImageView imageView;

            public ViewHolderBroadcastReceiver(ImageView _imageView)
            {
                imageView = _imageView;
            }

            public void setPosterURL(String _posterURL)
            {
                if(posterURL != null && posterURL.equals(_posterURL))
                    return;

                posterURL = _posterURL;

                if(!hasRegistered)
                {
                    hasRegistered = true;
                    LocalBroadcastManager.getInstance(imageView.getContext()).registerReceiver(this, new IntentFilter(VetraCoreData.INTENT_THUMB_AVAILABLE));
                }
            }

            @Override
            public void onReceive(Context context, Intent intent)
            {
                if(!hasRegistered)
                    return;

                File file = new File(context.getCacheDir(), posterURL);
                if(file.exists())
                {
                    imageView.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));

                    LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
                    hasRegistered = false;
                }
            }
        }

        public ViewHolder(LinearLayout v)
        {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.content_series_core);
            imageView = (ImageView) v.findViewById(R.id.row_image);
        }

        void setPosterURL(String posterURL)
        {
            Context context = mTextView.getContext();
            File file = new File(context.getCacheDir(), posterURL);
            if(file.exists())
            {
                imageView.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
            }

            else
            {
                if(receiver == null)
                    receiver = new ViewHolderBroadcastReceiver(imageView);

                receiver.setPosterURL(posterURL);
            }
        }
    }

    SimpleDateFormat dateJSONFormat = null;
    DateFormat dateOutputFormat = null;

    final Context context;
    private final int page;

    public VetraListAdapter(Context _context, int _page)
    {
        context = _context;
        page = _page;

        LocalBroadcastManager.getInstance(context).registerReceiver(new VetraAdapterUpdate(this), new IntentFilter(VetraCoreData.INTENT_DL_OVER));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        // create a new view
        LinearLayout layout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.content_series_row, parent, false);

        TextView textView = (TextView) layout.findViewById(R.id.content_series_core);

        textView.setTextColor(Color.BLACK);

        ViewHolder vh = new ViewHolder(layout);

        //Poster container
        View view = layout.findViewById(R.id.row_image);
        view.setBackgroundColor(Color.rgb(183, 193, 255));

        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
    {
        ViewHolder view = (ViewHolder) holder;
        final VetraDataStructure data = getStructure(position);
        if(data == null)
            return;

        final int dataID = data.ID.intValue();

        ((ViewHolder) holder).setPosterURL(data.posterURL);

        holder.itemView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                Context context = v.getContext();
                new VetraCoreData().prepareNewDetailedData(dataID);

                Intent intent = new Intent(context, VetraSeriesDetail.class);
                intent.putExtra(VetraListAdapter.TRANSACTIVITY_CONTAINER, dataID);
                context.startActivity(intent);
                ((Activity) context).overridePendingTransition(R.animator.enter_1, R.animator.enter_2);
            }
        });

        String htmlString = "<html><b>";

        htmlString += data.name;

        htmlString += "</b><br/>";

        htmlString += data.vote.toString() + " / 10";

        htmlString += "<br/>";

        if(dateJSONFormat == null)
            dateJSONFormat = new SimpleDateFormat("yyyy-MM-dd");

        if(dateOutputFormat == null)
            dateOutputFormat = new SimpleDateFormat(context.getResources().getString(R.string.format_date));

        try
        {
            Date date = dateJSONFormat.parse(data.initialAir);

            htmlString += context.getString(R.string.aired_since) + ": " + dateOutputFormat.format(date);
        } catch (ParseException ignored)
        {
        }

        htmlString += "<br/>";

        String genre = VetraCoreData.genres.get(data.genres.get(0));
        if(genre != null)
            htmlString += VetraCoreData.genres.get(data.genres.get(0));

        htmlString += "</html>";

        view.mTextView.setText(Html.fromHtml(htmlString, Html.FROM_HTML_MODE_COMPACT));
    }

    private VetraDataStructure getStructure(int rank)
    {
        VetraDataStructure output = null;

        SQLiteDatabase database = VetraCoreData.getDatabase();
        if(database != null)
        {
            String query;
            if(page == SeriesCoreFragment.FRAG_TOP)
            {
                query = "SELECT * FROM " + VetraSqliteNames.MAIN_TABLE_NAME_SERIES +
                        " WHERE " + VetraSqliteNames.MTSCN_RANK + " = " + Integer.toString(rank + 1);
            }
            else// if(page == SeriesCoreFragment.FRAG_FAVORITES)
            {
                query = "SELECT * FROM " + VetraSqliteNames.MAIN_TABLE_NAME_SERIES +
                        " WHERE " + VetraSqliteNames.MTSCN_ID + " IN " +
                        "(SELECT " + VetraSqliteNames.FAV_ID + " FROM " + VetraSqliteNames.TABLE_FAVORITES + " LIMIT 1 OFFSET " + Integer.toString(rank) + ")" +
                        " ORDER BY " + VetraSqliteNames.MTSCN_RANK + " LIMIT 1;";
            }

            Cursor cursor = database.rawQuery(query, null);

            if(cursor.getCount() == 1)
            {
                cursor.moveToFirst();
                output = new VetraDataStructure(cursor);
            }

            cursor.close();
            database.close();
        }

        return output;
    }

    @Override
    public int getItemCount()
    {
        SQLiteDatabase database = VetraCoreData.getDatabase();

        int count = 0;

        if(database != null)
        {
            String query;
            if(page == SeriesCoreFragment.FRAG_TOP)
                query = "SELECT COUNT(*) FROM " + VetraSqliteNames.MAIN_TABLE_NAME_SERIES +
                    " WHERE " + VetraSqliteNames.MTSCN_RANK + " != 0;";
            else
                query = "SELECT COUNT(*) FROM " + VetraSqliteNames.TABLE_FAVORITES;

            Cursor cursor = database.rawQuery(query, null);
            if(cursor.getCount() > 0)
            {
                cursor.moveToFirst();
                count = cursor.getInt(0);
            }
            cursor.close();
            database.close();
        }

        return count;
    }

    private class VetraAdapterUpdate extends BroadcastReceiver
    {
        final VetraListAdapter adapter;

        public VetraAdapterUpdate(VetraListAdapter _adapter)
        {
            adapter = _adapter;
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.hasExtra("favs") == (adapter.page == SeriesCoreFragment.FRAG_FAVORITES))
                adapter.notifyDataSetChanged();
        }
    }
}
