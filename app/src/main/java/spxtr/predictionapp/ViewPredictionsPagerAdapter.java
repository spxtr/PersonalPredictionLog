package spxtr.predictionapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ViewPredictionsPagerAdapter extends FragmentPagerAdapter {
    public ViewPredictionsPagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @Override
    @NonNull
    public Fragment getItem(int i) {
        ViewPredictionsFragment vpf = new ViewPredictionsFragment();
        Bundle b = new Bundle();
        b.putInt("role", i);
        vpf.setArguments(b);
        return vpf;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == ViewPredictionsFragment.ROLE_DUE) return "DUE";
        if (position == ViewPredictionsFragment.ROLE_UNRESOLVED) return "UNRESOLVED";
        return "ALL";
    }
}
