package fr.esiea.et.vetra;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.support.v4.app.FragmentPagerAdapter;

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

class VetraFragmentPagerAdapter extends FragmentPagerAdapter {

    private final int[] tabTitles = new int[] { R.string.tab_soon, R.string.tab_favs };
    private final Context context;

    public VetraFragmentPagerAdapter(FragmentManager fm, Context context)
    {
        super(fm);
        this.context = context;
    }

    @Override
    public int getCount()
    {
        int PAGE_COUNT = 2;
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position)
    {
        return SeriesCoreFragment.newInstance(position);
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        // Generate title based on item position
        return context.getResources().getString(tabTitles[position % 3]);
    }
}
