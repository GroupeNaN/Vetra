package fr.esiea.et.vetra;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
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

public class VetraSearchActivity extends AppCompatActivity
{
    VetraSearchAdapter adapter;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        adapter = new VetraSearchAdapter(this, null);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        if(intent.getAction().equals(Intent.ACTION_SEARCH))
        {
            String string = intent.getStringExtra(SearchManager.QUERY);
            if(string != null)
                VetraCoreData.performSearch(string, this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String searchQuery)
            {
                VetraCoreData.performSearch(searchQuery, VetraSearchActivity.this);
                return true;
            }
        });

        searchView.setIconifiedByDefault(false);

        searchView.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if(id == R.id.action_search)
            return true;

        return super.onOptionsItemSelected(item);
    }

    protected void getNewData(ArrayList<VetraDataStructure> data)
    {
        adapter.setNewData(data);
    }

    private class VetraSearchAdapter extends VetraListAdapter
    {
        ArrayList<VetraDataStructure> dataStructures;

        public VetraSearchAdapter(Context _context, ArrayList<VetraDataStructure> _data)
        {
            super(_context, SeriesCoreFragment.FRAG_TOP);
            dataStructures = _data;
        }

        void setNewData(ArrayList<VetraDataStructure> data)
        {
            dataStructures = data;
            this.notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            return new ViewHolder((LinearLayout) LayoutInflater.from(context).inflate(R.layout.search_row, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
        {
            ViewHolder view = (ViewHolder) holder;
            VetraDataStructure data = dataStructures.get(position);

            String htmlString = "<html><b>";

            htmlString += data.name;

            htmlString += "</b> ";

            if(dateJSONFormat == null)
                dateJSONFormat = new SimpleDateFormat("yyyy-MM-dd");

            try
            {
                Date date = dateJSONFormat.parse(data.initialAir);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);

                htmlString += "(" + calendar.get(Calendar.YEAR) + ")";
            } catch (ParseException ignored)
            {
            }

            htmlString += "</html>";

            view.mTextView.setText(Html.fromHtml(htmlString, Html.FROM_HTML_MODE_COMPACT));

            final int dataID = data.ID.intValue();
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
        }

        @Override
        public int getItemCount()
        {
            if(dataStructures != null)
                return dataStructures.size();
            return 0;
        }

    }
}
