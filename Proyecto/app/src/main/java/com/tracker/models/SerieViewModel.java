package com.tracker.models;

import android.content.Context;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tracker.controllers.RepositoryAPI;
import com.tracker.models.series.Serie;

public class SerieViewModel extends ViewModel {

    private MutableLiveData<Serie> serie;
    private RepositoryAPI repoTMDB;

    public LiveData<Serie> getSerie(View vista, int idSerie, Context context) {
            repoTMDB = RepositoryAPI.getInstance();
            serie = repoTMDB.getSerie(vista, idSerie, context);
        return serie;
    }

    public LiveData<Serie> getCurrentSerie(){
        return serie;
    }

}