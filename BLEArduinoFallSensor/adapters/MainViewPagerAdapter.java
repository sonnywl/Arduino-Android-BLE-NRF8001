
package com.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MainViewPagerAdapter extends FragmentPagerAdapter {

    Class<?>[] fragments;
    private final Context mContext;

    public MainViewPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
    }

    public MainViewPagerAdapter(FragmentManager fm, Context context, Class<?>[] fragments) {
        super(fm);
        this.fragments = fragments;
        this.mContext = context;
    }

    @Override
    public Fragment getItem(int pos) {
        return Fragment.instantiate(mContext, fragments[pos].getName(), null);
    }

    @Override
    public int getCount() {
        return fragments.length;
    }

}
