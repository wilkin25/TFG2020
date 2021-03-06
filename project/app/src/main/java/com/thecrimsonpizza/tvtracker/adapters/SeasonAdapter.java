package com.thecrimsonpizza.tvtracker.adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;
import com.thecrimsonpizza.tvtracker.R;
import com.thecrimsonpizza.tvtracker.data.FirebaseDb;
import com.thecrimsonpizza.tvtracker.models.seasons.Episode;
import com.thecrimsonpizza.tvtracker.models.seasons.Season;
import com.thecrimsonpizza.tvtracker.models.serie.SerieResponse;
import com.thecrimsonpizza.tvtracker.util.Constants;
import com.thecrimsonpizza.tvtracker.util.Util;

import java.util.Date;
import java.util.List;

import static com.thecrimsonpizza.tvtracker.util.Constants.BASE_URL_IMAGES_POSTER;

/**
 * Adapter for the RecyclerView that hosts the info of the seasons
 */
public class SeasonAdapter extends RecyclerView.Adapter<SeasonAdapter.ViewHolder> {

    private final Context mContext;
    private final List<Season> mSeasons;
    private final SerieResponse.Serie mSerie;
    private final List<SerieResponse.Serie> mFavs;

    public SeasonAdapter(Context mContext, SerieResponse.Serie serie, List<SerieResponse.Serie> favs) {
        this.mFavs = favs;
        this.mSerie = serie;
        this.mSeasons = serie.seasons;
        this.mContext = mContext;
        if (mSeasons != null) {
            Season.sortSeason(mSeasons);
        }
    }

    @NonNull
    @Override
    public SeasonAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return SeasonAdapter.ViewHolder.create(parent);
    }


    @Override
    public void onBindViewHolder(@NonNull SeasonAdapter.ViewHolder holder, final int position) {
        holder.bindTo(mSeasons.get(position), mSerie, mFavs, mContext);
    }

    @Override
    public int getItemCount() {
        if (mSeasons != null) {
            return mSeasons.size();
        }
        return 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final ImageView seasonPoster;
        final TextView seasonName;
        final TextView numEpisodes;
        final MaterialCheckBox watchedCheck;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            seasonPoster = itemView.findViewById(R.id.image_season);
            seasonName = itemView.findViewById(R.id.season_name);
            numEpisodes = itemView.findViewById(R.id.episode_number);
            watchedCheck = itemView.findViewById(R.id.checkbox_watched);

        }

        private void goToEpisodes(View view) {
            Bundle bundle = new Bundle();
            bundle.putInt(Constants.ID_SEASON, getAdapterPosition());
            Navigation.findNavController(view).navigate(R.id.action_series_to_episodes, bundle);
        }

        static SeasonAdapter.ViewHolder create(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_season, parent, false);
            return new SeasonAdapter.ViewHolder(view);
        }

        void bindTo(Season season, SerieResponse.Serie serie, List<SerieResponse.Serie> mFavs, Context context) {
            if (season != null) {
                itemView.setOnClickListener(this::goToEpisodes);

                if (serie.added) setWatchCheck(season, serie, mFavs);

                seasonName.setText(season.name);
                Util.getImage(BASE_URL_IMAGES_POSTER + season.posterPath, seasonPoster, context);

                if (season.episodes != null) {
                    if (serie.added)
                        numEpisodes.setText(context.getResources().getQuantityString(R.plurals.num_episodes_follow,
                                season.episodes.size(),
                                countEpisodes(season),
                                season.episodes.size()));
                    else
                        numEpisodes.setText(context.getResources().getQuantityString(R.plurals.n_episodes, season.episodes.size(), season.episodes.size()));

                } else numEpisodes.setText(context.getString(R.string.no_data));

            }
        }

        /**
         * Checks if the episode is watched and sets it true or false the CheckBox
         *
         * @param season season of the serie
         * @param serie  serie loaded in the SerieFragment
         * @param favs   list of series in the following list
         */
        private void setWatchCheck(Season season, SerieResponse.Serie serie, List<SerieResponse.Serie> favs) {
            watchedCheck.setVisibility(View.VISIBLE);
            watchedCheck.setChecked(season.visto);
            watchedCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!season.visto) watchSeason(serie, favs, getAdapterPosition());
                } else if (season.visto) unwatchSeason(serie, favs, getAdapterPosition());
            });
        }

        int countEpisodes(Season season) {
            int cont = 0;
            for (Episode e : season.episodes) {
                if (e.visto) cont++;
            }
            return cont;
        }

        /**
         * Checks if the season is finished or not
         *
         * @param serie we need its seasons
         * @return true or false
         */
        private boolean checkSeasonFinished(SerieResponse.Serie serie) {
            for (Season s : serie.seasons) {
                if (!s.visto) return false;
            }
            return true;
        }

        /**
         * Set as watched the whole season in the RecyclerView and then in the Database
         *
         * @param serie     serie loaded in the SerieFragment
         * @param favs      list of series in the following list
         * @param posSeason season position in the RecyclerView
         */
        private void watchSeason(SerieResponse.Serie serie, List<SerieResponse.Serie> favs, int posSeason) {
            int pos = serie.getPosition(favs);
            Season.sortSeason(favs.get(pos).seasons);

//            MARCAR TEMPORADA
            favs.get(pos).seasons.get(posSeason).visto = true;
            favs.get(pos).seasons.get(posSeason).watchedDate = new Date();

//            MARCAR EPISODIOS
            for (Episode e : favs.get(pos).seasons.get(posSeason).episodes) {
                e.visto = true;
                e.watchedDate = new Date();
            }

//            MARCAR SERIE
            if (checkSeasonFinished(favs.get(pos))) {
                favs.get(pos).finished = true;
                favs.get(pos).finishDate = new Date();
            } else {
                favs.get(pos).finished = false;
                favs.get(pos).finishDate = null;
            }

//            GUARDAR FAVORITOS EN FIREBASE
            FirebaseDb.getInstance(FirebaseAuth.getInstance().getCurrentUser()).setSeriesFav(favs);
        }

        /**
         * Set as unwatched the whole season in the RecyclerView and then in the Database
         *
         * @param serie     serie loaded in the SerieFragment
         * @param favs      list of series in the following list
         * @param posSeason season position in the RecyclerView
         */
        private void unwatchSeason(SerieResponse.Serie serie, List<SerieResponse.Serie> favs, int posSeason) {
            int pos = serie.getPosition(favs);
            Season.sortSeason(favs.get(pos).seasons);

//            UNWATCH TEMPORADA
            favs.get(pos).seasons.get(posSeason).visto = false;
            favs.get(pos).seasons.get(posSeason).watchedDate = null;

//            UNWATCH EPISODIOS
            for (Episode e : favs.get(pos).seasons.get(posSeason).episodes) {
                e.visto = false;
                e.watchedDate = null;
            }

//            UNWATCH SERIE
            favs.get(pos).finished = false;
            favs.get(pos).finishDate = null;

            FirebaseDb.getInstance(FirebaseAuth.getInstance().getCurrentUser()).setSeriesFav(favs);
        }
    }
}
