package com.abstractwombat.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;

public class FragmentTest extends ActivityInstrumentationTestCase2<FragmentUtilActivity> {
    private FragmentTest fragment;
    private Activity mActivity;

    public FragmentTest() {
        super(FragmentUtilActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
//        OrderedListFragment f = OrderedListFragment.newInstance(new String[]{"One", "Two", "Three"}, 0);
//        mActivity = getActivity();
//        getActivity().getFragmentManager().beginTransaction().add(1, (Fragment)f, null).commit();
    }

    @Test
    public void testPreconditions() {
        assertNotNull("Activity was null", mActivity);
    }
}