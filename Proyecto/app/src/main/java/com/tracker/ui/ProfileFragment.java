package com.tracker.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.anychart.APIlib;
import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.charts.Pie;
import com.anychart.charts.TagCloud;
import com.anychart.scales.OrdinalColor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.tracker.R;
import com.tracker.models.serie.SerieResponse;
import com.tracker.repositories.FirebaseDb;
import com.tracker.util.Stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProfileFragment extends Fragment {

    private List<SerieResponse.Serie> mFavs = new ArrayList<>();
    private Context mContext;
    private static final String[] COLORS = {"#48c9b0", "#2DAEA9", "#23949C", "#28798A", "#2E6072", "#2F4858", "#00B7C3", "#00A2D2", "#0089D5", "#446BC5"};

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getFollowingSeries(view);

    }

    private void initGraficoPie(View view) {
        AnyChartView pieChart = view.findViewById(R.id.pie_chart);
        APIlib.getInstance().setActiveAnyChartView(pieChart);
        pieChart.setBackgroundColor("#00000000");
        pieChart.setProgressBar(view.findViewById(R.id.progress_pie));

        Pie pie = AnyChart.pie();

        List<DataEntry> data = Stats.getInstance(mContext).getPieGenres(mFavs);
        pie.data(data);
        pie.title().enabled(false);
//        pie.labels().position("outside");
        pie.labels().fontSize(15);
        pie.labels().fontColor("white");
        pie.labels().fontWeight(700);
        pie.legend().enabled(false);
        pie.palette(COLORS);
        pie.background().fill("rgba(0,0,255,0)");
        pieChart.setChart(pie);
    }


    void setBubble(View view) {
        AnyChartView tagChart = view.findViewById(R.id.buble_chart);
        APIlib.getInstance().setActiveAnyChartView(tagChart);
        tagChart.setBackgroundColor("#00000000");
        tagChart.setProgressBar(view.findViewById(R.id.progress_buble));

        TagCloud tagCloud = AnyChart.tagCloud();

        tagCloud.title().enabled(false);
        tagCloud.legend().enabled(false);

        OrdinalColor ordinalColor = OrdinalColor.instantiate();
        ordinalColor.colors(new String[]{
                "#26959f", "#f18126", "#3b8ad8", "#60727b", "#e24b26"
        });
        tagCloud.colorScale(ordinalColor);
        tagCloud.angles(new Double[]{-90d, 0d, 90d});
        tagCloud.colorRange().enabled(false);

        tagCloud.normal().fontFamily("Arial");


        List<DataEntry> data = Stats.getInstance(mContext).geNetworks(mFavs);

        tagCloud.background().fill("rgba(0,0,255,0)");
        tagCloud.data(data);

        tagChart.setChart(tagCloud);
    }

    private void getFollowingSeries(View view) {
        FirebaseDb.getInstance(FirebaseAuth.getInstance().getCurrentUser()).getSeriesFav().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mFavs.clear();
                mFavs.addAll(Objects.requireNonNull(dataSnapshot.getValue(new GenericTypeIndicator<List<SerieResponse.Serie>>() {
                })));

                initGraficoPie(view);
                setBubble(view);

                TextView total = view.findViewById(R.id.total_time);
                TextView num = view.findViewById(R.id.num_series);
                TextView epis = view.findViewById(R.id.eps_vistos);
                TextView max = view.findViewById(R.id.serie_max);

                total.setText(Stats.getInstance(mContext).countTimeEpisodesWatched(mFavs));
                num.setText(Stats.getInstance(mContext).countSeries(mFavs));
                epis.setText(Stats.getInstance(mContext).countNumberEpisodesWatched(mFavs));
                max.setText(Stats.getInstance(mContext).mostWatchedSerie(mFavs));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
