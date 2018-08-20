package com.example.space.hkm.ui;



import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.design.widget.TabLayout;
import android.content.Context;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.example.space.hkm.App;
import com.example.space.hkm.LoginManager;
import com.example.space.hkm.R;
import com.example.space.hkm.helpers.Logger;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class TabLayoutOTM extends OTMActionBarActivity {

    private static final String SELECTED_TAB = "TAB";

    private static final String MAIN_MAP = "MainMapActivity";
    private static final String PROFILE = "ProfileDisplay";
    private static final String LISTS = "ListDisplay";
    private static final String ABOUT = "AboutDisplay";
    private Menu menu;
    private DrawerLayout mDrawerLayout;
    private static  int CurrentMenuItemIndex= 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);

        android.support.v7.app.ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setHomeAsUpIndicator(R.drawable.ic_home);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View vi = inflater.inflate(R.layout.navheader,navigationView);
        Menu menu = navigationView.getMenu();


        TextView userNameView = (TextView) vi.findViewById(R.id.username);
        TextView nameView = (TextView) vi.findViewById(R.id.name);

        LoginManager loginManager = App.getLoginManager();

        try {

                userNameView.setText(loginManager.loggedInUser.getUserName());
                nameView.setText(loginManager.loggedInUser.getFirstName()+' '+loginManager.loggedInUser.getLastName());
                menu.findItem(R.id.current_map).setTitle(App.getCurrentInstance().getName());
                menu.findItem(R.id.user_phone_no).setTitle(loginManager.loggedInUser.getZipcode());
                menu.getItem(1).setChecked(true);




            } catch (JSONException e) {
                Logger.error("Could not get user details.", e);
            }

//        TabLayout tabs =  (TabLayout)findViewById(R.id.tabs);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);



        navigationView.setNavigationItemSelectedListener(

                new NavigationView.OnNavigationItemSelectedListener() {
                    // This method will trigger on item Click of navigation menu
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // Set item in checked state
                        menuItem.setChecked(true);
                        // TODO: handle navigation

                        int id = menuItem.getItemId();


                        if (id == R.id.action_profile) {
//                            final FragmentManager manager = TabLayoutOTM.this.getSupportFragmentManager();
//                            Fragment tabFragment = Fragment.instantiate(TabLayoutOTM.this, ProfileDisplay.class.getName());
//                            FragmentTransaction  ft = manager.beginTransaction();
//                            ft.replace(tabFragment);
//                            ft.commit();
//                            viewPager.setCurrentItem(1);
                            CurrentMenuItemIndex= 3;
                            startActivity(new Intent(TabLayoutOTM.this, ChangePassword.class));

                        }else if (id == R.id.action_logout) {
                            logout();
                        }
                        else if (id == R.id.switch_map){
                            startActivity(new Intent(TabLayoutOTM.this, InstanceSwitcherActivity.class));
                        }

                        // Closing drawer on item click
                        mDrawerLayout.closeDrawers();
                        return true;
                    }
                });

//        tabs.setupWithViewPager(viewPager);
//        tabs.addTab(tabs.newTab().setText(R.string.tab_map).setTag(MAIN_MAP));
//        tabs.addTab(tabs.newTab().setText(R.string.profile_text));
//        tabs.addTab(tabs.newTab().setText("Tab 1"));
//        tabs.addTab(tabs.newTab().setText("Tab 2"));
//        tabs.addTab(tabs.newTab().setText("Tab 3"));
//
//        ActionBar actionBar = getActionBar();
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//
//        actionBar.addTab(
//                actionBar.newTab()
//                        .setText(R.string.tab_map)
//                        .setTag(MAIN_MAP)
//                        .setTabListener(new TabListener<>(this, MAIN_MAP, MainMapFragment.class))
//        );
//
//        actionBar.addTab(
//                actionBar.newTab()
//                        .setText(R.string.tab_profile)
//                        .setTabListener(new TabListener<>(this, PROFILE, ProfileDisplay.class))
//        );
//
//        actionBar.addTab(
//                actionBar.newTab()
//                        .setText(R.string.tab_lists)
//                        .setTabListener(new TabListener<>(this, LISTS, ListDisplay.class))
//        );
//
//        actionBar.addTab(
//                actionBar.newTab()
//                        .setText(R.string.tab_about)
//                        .setTabListener(new TabListener<>(this, ABOUT, AboutDisplay.class))
//        );

//        if (savedInstanceState != null) {
//            actionBar.setSelectedNavigationItem(savedInstanceState.getInt(SELECTED_TAB));
//        }
    }

    private void setupViewPager(ViewPager viewPager) {
        Adapter adapter = new Adapter(getSupportFragmentManager());
        adapter.addFragment(new MainMapFragment(), getResources().getString(R.string.tab_map));
        adapter.addFragment(new ProfileDisplay(), getResources().getString(R.string.tab_profile));
        viewPager.setAdapter(adapter);
    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public Adapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
////        this.menu = menu;
////        return super.onCreateOptionsMenu(menu);
//
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.main_edit_menu, menu);
//
////
//
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
        return super.onOptionsItemSelected(item);
    }



//    @Override
//    public boolean onMenuItemSelected(int featureId,MenuItem item) {
//        // Handle presses on the action bar items
//        int id = item.getItemId();
//        if (id == R.id.action_logout) {
//            logout();
//            return true;
//        }
//        else if(id == R.id.action_profile){
//
//            final FragmentManager manager = TabLayoutOTM.this.getFragmentManager();
//            Fragment tabFragment = Fragment.instantiate(this, ProfileDisplay.class.getName());
//              FragmentTransaction  ft = manager.beginTransaction();
//                ft.show(tabFragment);
//                ft.commit();
//                return true;
//
//
//        }
////        } else if (id == R.id.edit_tree_picture) {
////            changeTreePhoto();
////            return true;
////        } else if (id == android.R.id.home) {
////            cancel();
////            return true;
////        }
//        return super.onMenuItemSelected(featureId,item);
//    }

    public void logout(){
        App.getLoginManager().logOut(this);
    }
//
//    @Override
//    public void onBackPressed() {
//        // A bit of an annoyance, the TabLayout Activity gets the backpress events
//        // and must delegate them back down to the MainMapActivity Fragment
//        // If we need to support handling back presses differently on each tab,
//        // we should probably make an Interface and call whatever the current tab is
//        ActionBar actionBar = getActionBar();
//        if (actionBar.getSelectedTab().getTag() == MAIN_MAP) {
//            final FragmentManager manager = TabLayoutOTM.this.getFragmentManager();
//            MainMapFragment mainMap = (MainMapFragment) manager.findFragmentByTag(MAIN_MAP);
//            if (mainMap.shouldHandleBackPress()) {
//                mainMap.onBackPressed();
//            } else {
//                super.onBackPressed();
//            }
//        } else {
//            super.onBackPressed();
//        }
//    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt(SELECTED_TAB, CurrentMenuItemIndex);
    }

//    public class TabListener<T extends Fragment> implements ActionBar.TabListener {
//        private Fragment tabFragment;
//        private final Activity host;
//        private final String tag;
//        private final Class<T> tabClass;
//
//        /**
//         * Constructor used each time a new tab is created.
//         *
//         * @param host The host Activity, used to instantiate the fragment
//         * @param tag  The identifier tag for the fragment
//         * @param clz  The fragment's Class, used to instantiate the fragment
//         */
//        public TabListener(Activity host, String tag, Class<T> clz) {
//            this.host = host;
//            this.tag = tag;
//            tabClass = clz;
//
//            final FragmentManager manager = TabLayout.this.getFragmentManager();
//            tabFragment = manager.findFragmentByTag(tag);
//            if (tabFragment != null && !tabFragment.isHidden()) {
//                manager.beginTransaction().hide(tabFragment).commit();
//            }
//        }
//
//        @Override
//        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
//            // Check if the fragment is already initialized
//            if (tabFragment == null) {
//                // If not, instantiate and add it to the activity
//                tabFragment = Fragment.instantiate(host, tabClass.getName());
//                ft.add(android.R.id.content, tabFragment, tag);
//            } else {
//                // If it exists, simply attach it in order to show it
//                ft.show(tabFragment);
//            }
////            App.getAppInstance().sendFragmentView(tabFragment, TabLayout.this);
//        }
//
//        @Override
//        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
//            if (tabFragment != null) {
//                // Detach the fragment, because another one is being attached
//                ft.hide(tabFragment);
//            }
//            // Hide the soft keyboard if it is up
//            View currentView = getCurrentFocus();
//            if (currentView != null) {
//                InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                im.hideSoftInputFromWindow(currentView.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
//            }
//            if (menu != null) {
//                menu.clear();
//            }
//        }
//
//        @Override
//        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
//        }
//    }
}
