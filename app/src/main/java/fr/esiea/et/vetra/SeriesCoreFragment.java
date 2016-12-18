package fr.esiea.et.vetra;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

public class SeriesCoreFragment extends Fragment {

    public static final int FRAG_TOP = 0;
    public static final int FRAG_FAVORITES = 1;

    private int _page;

    public SeriesCoreFragment()
    {
        SeriesCore context = (SeriesCore) this.getContext();
        if(context != null)
        {
            _page = ((ViewPager) context.findViewById(R.id.viewpager)).getCurrentItem();
        }
    }

    public static SeriesCoreFragment newInstance(int page)
    {
        SeriesCoreFragment fragment = new SeriesCoreFragment();

        fragment.setPage(page);

        return fragment;
    }

    private void setPage(int page)
    {
        _page = page;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View internalView = inflater.inflate(R.layout.fragment_series_core, container, false);

        RecyclerView list = (RecyclerView) internalView.findViewById(R.id.list);

        list.setAdapter(new VetraListAdapter(getContext(), _page));
        list.setLayoutManager(new LinearLayoutManager(this.getContext()));

        return internalView;
    }
}
