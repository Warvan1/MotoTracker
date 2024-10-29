package com.example.mototracker;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnFragmentInteractionListener {
    private DrawerLayout _drawerLayout;
    private Toolbar _toolbar;
    private NavigationView _navigationView;
    private FragmentSwitcher _fragmentSwitcher;
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

        //initialize the fragment switcher object
        _fragmentSwitcher = FragmentSwitcher.createInstance(_navigationView.getMenu());

        if(savedInstanceState == null){
            //set the initial fragment to the home fragment on first launch
            _fragmentSwitcher.switchFragment(new HomeFragment(), getSupportFragmentManager());
        }

        //listener for when a item is selected in the drawer menu
        _navigationView.setNavigationItemSelectedListener(menuItem -> {
            //close the menu after selecting an option
            _drawerLayout.closeDrawer(GravityCompat.START);
            //extract the menuID for the selected item
            int menuID = menuItem.getItemId();

            //handle all the different menu options
            if(menuID == R.id.home){
                Log.d("code", "home");
                _fragmentSwitcher.switchFragment(new HomeFragment(), getSupportFragmentManager());
            }
            else if (menuID == R.id.dashboard) {
                Log.d("code", "dashboard");
                if(_auth0.isAuthenticated()){
                    _fragmentSwitcher.switchFragment(new DashboardFragment(), getSupportFragmentManager());
                }
                else{
                    Toast.makeText(this, "Login to view Dashboard", Toast.LENGTH_LONG).show();
                }
            }
            else if (menuID == R.id.maintenanceLog) {
                Log.d("code", "maintenance log");
                if(_auth0.isAuthenticated()){
                    _fragmentSwitcher.switchFragment(new MaintenanceLogFragment(), getSupportFragmentManager());
                }
                else{
                    Toast.makeText(this, "Login to view Maintenance Log", Toast.LENGTH_LONG).show();
                }
            }
            else if (menuID == R.id.statistics) {
                Log.d("code", "stats");
                if(_auth0.isAuthenticated()){
                    _fragmentSwitcher.switchFragment(new StatisticsFragment(), getSupportFragmentManager());
                }
                else{
                    Toast.makeText(this, "Login to view Statistics", Toast.LENGTH_LONG).show();
                }
            }
            else if (menuID == R.id.login) {
                Log.d("code", "login");
                _auth0.handleLoginOrLogout(v -> {
                    handleAuth();
                });
            }
            else if (menuID == R.id.carManager) {
                Log.d("code", "Car Manager");
                if(_auth0.isAuthenticated()){
                    _fragmentSwitcher.switchFragment(new CarManagerFragment(), getSupportFragmentManager());
                }
                else{
                    Toast.makeText(this, "Login to view Car Manager", Toast.LENGTH_LONG).show();
                }
            }

            return true;
        });
    }

    private void handleAuth(){
        MenuItem menuItem = _navigationView.getMenu().findItem(R.id.login);
        TextView emailT = findViewById(R.id.drawer_header_email);
        if(_auth0.isAuthenticated()){
            _userProfile = _auth0.getUserProfile();
            emailT.setText(_userProfile.getString("email"));
            menuItem.setTitle(R.string.logout);
            menuItem.setIcon(getDrawable(R.drawable.baseline_logout_24));
            //make an adduser post request
            new HTTPRequest(this, getString(R.string.api_base_url) + "/adduser").setMethod("POST")
                    .setAuthToken(_auth0.getAccessToken()).setData(_userProfile).runAsync();
            //switch to the dashboard fragment on login if we are on the home fragment
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.flFragment);
            if(currentFragment instanceof HomeFragment) {
                _fragmentSwitcher.switchFragment(new DashboardFragment(), getSupportFragmentManager());
            }
        }
        else{
            _userProfile = null;
            emailT.setText(R.string.emailPlaceholder);
            menuItem.setTitle(R.string.login);
            menuItem.setIcon(getDrawable(R.drawable.baseline_login_24));
            //switch to the home fragment on logout if you are NOT on the home fragment
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.flFragment);
            _fragmentSwitcher.switchFragment(new HomeFragment(), getSupportFragmentManager());
        }
    }

    @Override
    public void onHomeHandleAuth() {
        _auth0.handleLoginOrLogout(v -> {
            handleAuth();
        });
    }
}