package com.binish.smstesting.Navigation;

import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.binish.smstesting.Fragments.HomeScreen;
import com.binish.smstesting.Fragments.ViewLogs;
import com.binish.smstesting.Models.SimInfo;
import com.binish.smstesting.R;
import com.binish.smstesting.Utils.SimUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.work.WorkManager;

import static com.binish.smstesting.Fragments.HomeScreen.simID;

public class SMSBulkNavigation extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    FragmentManager fragmentManager;
    Fragment homeScreen;
    Fragment viewLogs;
    NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smsbulk_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        homeScreen = new HomeScreen();
        viewLogs = new ViewLogs();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragmentScreen, homeScreen, "HOME_SCREEN").addToBackStack(null).commit();
        navigationView.setCheckedItem(R.id.navSendMessage);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            int count = getSupportFragmentManager().getBackStackEntryCount();
            Fragment homeScreenFragment = getSupportFragmentManager().findFragmentByTag("HOME_SCREEN");
            if (count == 0 || (homeScreenFragment!=null && homeScreenFragment.isVisible())) {
                super.onBackPressed();
            } else {
                getSupportFragmentManager().popBackStack();

                Fragment myFragment = getSupportFragmentManager().findFragmentByTag("VIEW_LOGS");
                if (myFragment != null && myFragment.isVisible()) {
                    navigationView.setCheckedItem(R.id.navSendMessage);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.smsbulk_navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.sendAgain) {

        }


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.navSendMessage) {
            try {
                if (!(fragmentManager.findFragmentByTag("HOME_SCREEN")).isVisible())
                    fragmentManager.beginTransaction().replace(R.id.fragmentScreen, homeScreen, "HOME_SCREEN").addToBackStack(null).commit();
            } catch (Exception e) {
                fragmentManager.beginTransaction().replace(R.id.fragmentScreen, homeScreen, "HOME_SCREEN").addToBackStack(null).commit();
            }
        } else if (id == R.id.navLogs) {
            fragmentManager.beginTransaction().replace(R.id.fragmentScreen, viewLogs, "VIEW_LOGS").addToBackStack(null).commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
