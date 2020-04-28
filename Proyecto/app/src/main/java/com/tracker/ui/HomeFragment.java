package com.tracker.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.tracker.R;
import com.tracker.adapters.HomeAdapter;
import com.tracker.models.BasicResponse;
import com.tracker.models.serie.SerieResponse;
import com.tracker.repositories.FirebaseDb;
import com.tracker.repositories.TmdbRepository;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

public class HomeFragment extends Fragment {

    private List<BasicResponse.SerieBasic> mPopulares = new ArrayList<>();
    private List<BasicResponse.SerieBasic> mNuevas = new ArrayList<>();
    private List<BasicResponse.SerieBasic> mFavs = new ArrayList<>();
    private Context mContext;
    private boolean isOn;

    private HomeAdapter adapterPopular;
    private HomeAdapter adapterNueva;
    private HomeAdapter adapterFav;
    private ViewSwitcher switcherFavs;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        adapterPopular = new HomeAdapter(getActivity(), mPopulares);
        adapterNueva = new HomeAdapter(getActivity(), mNuevas);
        adapterFav = new HomeAdapter(getActivity(), mFavs);

        switcherFavs = view.findViewById(R.id.switcher_favs);
        RecyclerView rvPopulares = view.findViewById(R.id.gridPopulares);
        RecyclerView rvNuevas = view.findViewById(R.id.gridNuevas);
        RecyclerView rvFavs = view.findViewById(R.id.gridFavs);

        initRecycler(rvNuevas, adapterNueva);
        initRecycler(rvPopulares, adapterPopular);
        initRecycler(rvFavs, adapterFav);

        initHome(view);
    }

    private void initHome(@NonNull View view) {
        if (getConnectivityStatus()) {
            isOn = true;
            getTrending(adapterPopular);
            getNew(adapterNueva);
            getFavorites(adapterFav, switcherFavs);
        } else {
            isOn = false;
            Snackbar.make(view, getString(R.string.no_conn), BaseTransientBottomBar.LENGTH_INDEFINITE)
                    .setAction(R.string.activate_net, v1 -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS))).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isOn) {
            initHome(requireView());
        }
    }

    private void getFavorites(HomeAdapter adapterFav, ViewSwitcher switcherFavs) {
        FirebaseDb.getInstance(FirebaseAuth.getInstance().getCurrentUser()).getSeriesFav().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mFavs.clear();
                GenericTypeIndicator<List<SerieResponse.Serie>> genericTypeIndicator = new GenericTypeIndicator<List<SerieResponse.Serie>>() {
                };
                List<SerieResponse.Serie> favTemp = dataSnapshot.getValue(genericTypeIndicator);
                if (favTemp != null) {
                    for (SerieResponse.Serie fav : favTemp) {
                        mFavs.add(fav.toBasic());
                    }
                    adapterFav.notifyDataSetChanged();
                    if (R.id.gridFavs == switcherFavs.getNextView().getId())
                        switcherFavs.showNext();
                } else {
                    if (R.id.no_data_favs == switcherFavs.getNextView().getId())
                        switcherFavs.showNext();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void getNew(HomeAdapter adapterNueva) {
        TmdbRepository.getInstance().getNewSeries()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(Throwable::printStackTrace)
                .subscribe(series -> refreshData(mNuevas, adapterNueva, series.basicSeries));
    }

    private void getTrending(HomeAdapter adapterPopular) {
        TmdbRepository.getInstance().getTrendingSeries()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(Throwable::printStackTrace)
                .subscribe(series -> refreshData(mPopulares, adapterPopular, series.basicSeries));
    }

    private void refreshData(List<BasicResponse.SerieBasic> lista, HomeAdapter adapter, List<BasicResponse.SerieBasic> response) {
        lista.clear();
        lista.addAll(response);
        adapter.notifyDataSetChanged();
    }

    private void initRecycler(RecyclerView rv, HomeAdapter adapter) {
        rv.setHasFixedSize(true);
        rv.setItemViewCacheSize(20);
        rv.setSaveEnabled(true);
        rv.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);
    }

    private boolean getConnectivityStatus() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        if (capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) {
            return true;
        }
//        showToastMessage(getString(R.string.no_network));
        return false;
    }
}