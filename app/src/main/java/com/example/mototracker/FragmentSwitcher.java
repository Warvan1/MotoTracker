package com.example.mototracker;

import android.view.Menu;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class FragmentSwitcher {
    private static FragmentSwitcher _fragmentSwitcherObject;
    public Menu _menu;

    private FragmentSwitcher(Menu menu){
        _menu = menu;
        _fragmentSwitcherObject = this;
    }
    public static FragmentSwitcher createInstance(Menu menu){
        if(_fragmentSwitcherObject == null) {
            _fragmentSwitcherObject = new FragmentSwitcher(menu);
        }
        return _fragmentSwitcherObject;
    }
    public static FragmentSwitcher getInstance(){
        return _fragmentSwitcherObject;
    }

    public void switchFragment(Fragment fragment, FragmentManager fragmentManager){
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.flFragment, fragment);
        transaction.commit();

        for (int i = 0; i < _menu.size(); i++){
            _menu.getItem(i).setChecked(false);
        }
        if(fragment instanceof HomeFragment){
            _menu.findItem(R.id.home).setChecked(true);
        }
        else if(fragment instanceof DashboardFragment){
            _menu.findItem(R.id.dashboard).setChecked(true);
        }
        else if(fragment instanceof StatisticsFragment){
            _menu.findItem(R.id.statistics).setChecked(true);
        }
        else if(fragment instanceof MaintenanceLogFragment){
            _menu.findItem(R.id.maintenanceLog).setChecked(true);
        }
        else if(fragment instanceof CarManagerFragment){
            _menu.findItem(R.id.carManager).setChecked(true);
        }

    }
}
