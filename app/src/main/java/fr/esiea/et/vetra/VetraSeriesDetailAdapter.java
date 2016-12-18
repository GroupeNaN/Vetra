package fr.esiea.et.vetra;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;

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

public class VetraSeriesDetailAdapter extends VetraListAdapter
{
    private final VetraDataStructure data;

    public VetraSeriesDetailAdapter(Context _context, VetraDataStructure _data)
    {
        super(_context, SeriesCoreFragment.FRAG_TOP);
        data = _data;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
    {
        ViewHolder view = (ViewHolder) holder;

        ((ViewHolder) holder).setPosterURL((String) data.seasons.get(position).get("posterURL"));

        String htmlString = "<html><b>";

        htmlString += context.getString(R.string.detail_season) + " " + Integer.toString(((Number) data.seasons.get(position).get("seasonNumber")).intValue() + 1);

        htmlString += "</b><br/>";

        htmlString += data.seasons.get(position).get("episodeCount").toString() + " " + context.getString(R.string.detail_episodes);

        htmlString += "<br/>";

        if(dateJSONFormat == null)
            dateJSONFormat = new SimpleDateFormat("yyyy-MM-dd");

        if(dateOutputFormat == null)
            dateOutputFormat = new SimpleDateFormat(context.getResources().getString(R.string.format_date));

        try
        {
            Date date = dateJSONFormat.parse((String) data.seasons.get(position).get("initialAir"));

            htmlString += context.getString(R.string.detail_aired_on) + " " + dateOutputFormat.format(date);
        } catch (ParseException ignored)
        {
        }

        htmlString += "</html>";

        view.mTextView.setText(Html.fromHtml(htmlString, Html.FROM_HTML_MODE_COMPACT));
    }

    @Override
    public int getItemCount()
    {
        return data.seasons.size();
    }
}
