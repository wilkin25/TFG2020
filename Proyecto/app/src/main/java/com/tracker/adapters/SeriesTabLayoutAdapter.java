package com.tracker.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.tracker.ui.CastFragment;
import com.tracker.ui.FavoritosFragment;
import com.tracker.ui.SinopsisFragment;

import org.jetbrains.annotations.NotNull;

public class SeriesTabLayoutAdapter extends FragmentStateAdapter {

    public SeriesTabLayoutAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NotNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new SinopsisFragment();
        } else if (position == 1) {
            return new CastFragment();
        } else if (position == 2) {
            return new FavoritosFragment();
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}