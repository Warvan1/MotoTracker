package com.example.mototracker;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.internal.NavigationMenuItemView;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {
    DrawerLayout _drawerLayout;
    Toolbar _toolbar;
    NavigationView _navigationView;

    private Auth0Authentication _auth0;
    private JSONObjectWrapper _userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //initialize the auth0 object
        _auth0 = Auth0Authentication.getInstance(this);

        //initialize other objects
        _drawerLayout = findViewById(R.id.drawerLayout);
        _toolbar = findViewById(R.id.toolbar);
        _navigationView = findViewById(R.id.nav);

        //set the toolbar title
        _toolbar.setTitle(R.string.app_name);
        //create the hamburger menu open and close toggle on the toolbar for the drawer nav
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, _drawerLayout, _toolbar, R.string.open, R.string.close);
        _drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        //set the hamburger menu color
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.md_theme_primary));

        //set the initial fragment to the home fragment
        fragmentSwitcher(new HomeFragment());

        //create a drawer listener to callback drawer opening and other events
        _drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                handleAuth();
            }
        });

        //listener for when a item is selected in the drawer menu
        _navigationView.setNavigationItemSelectedListener(menuItem -> {
            //close the menu after selecting an option
            _drawerLayout.closeDrawer(GravityCompat.START);
            //extract the menuID for the selected item
            int menuID = menuItem.getItemId();

            //handle all the different menu options
            if(menuID == R.id.home){
                Log.d("code", "home");
                fragmentSwitcher(new HomeFragment());
            }
            else if (menuID == R.id.dashboard) {
                Log.d("code", "dashboard");
                if(_auth0.isAuthenticated()){
                    fragmentSwitcher(new DashboardFragment());
                }
                else{
                    Toast.makeText(this, "Login to view Dashboard", Toast.LENGTH_LONG).show();
                }
            }
            else if (menuID == R.id.maintenanceLog) {
                Log.d("code", "maintenance log");
            }
            else if (menuID == R.id.statistics) {
                Log.d("code", "stats");
            }
            else if (menuID == R.id.login) {
                Log.d("code", "login");
                _auth0.handleLoginOrLogout(v -> {
                    handleAuth();
                    //we are doing this not in the handleAuth function because we only want it to run on initial login
                    if(_auth0.isAuthenticated()) {
                        //switch to the dashboard fragment on login if we are on the home fragment
                        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.flFragment);
                        if(currentFragment instanceof HomeFragment) {
                            fragmentSwitcher(new DashboardFragment());
                        }
                    }
                });
            }
            else if (menuID == R.id.settings) {
                Log.d("code", "settings");
            }
            else if (menuID == R.id.carManager) {
                Log.d("code", "Car Manager");
            }

            return true;
        });
    }

    private void handleAuth(){
        Menu menu = _navigationView.getMenu();
        MenuItem menuItem = menu.findItem(R.id.login);
        TextView emailT = findViewById(R.id.drawer_header_email);
        if(_auth0.isAuthenticated()){
            if(_userProfile == null){
                _userProfile = _auth0.getUserProfile();
            }
            emailT.setText(_userProfile.getString("email"));
            menuItem.setTitle(R.string.logout);
        }
        else{
            _userProfile = null;
            emailT.setText(R.string.emailPlaceholder);
            menuItem.setTitle(R.string.login);
            //switch to the home fragment on logout if you are NOT on the home fragment
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.flFragment);
            if(!(currentFragment instanceof HomeFragment)){
                fragmentSwitcher(new HomeFragment());
            }
        }
    }

    public void fragmentSwitcher(Fragment fragment){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.flFragment, fragment);
        transaction.commit();
    }
}