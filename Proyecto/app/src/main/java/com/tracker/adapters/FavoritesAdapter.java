package com.tracker.adapters;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.tracker.R;
import com.tracker.models.seasons.Episode;
import com.tracker.models.seasons.Season;
import com.tracker.models.serie.SerieResponse;
import com.tracker.util.Util;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.tracker.util.Constants.BASE_URL_IMAGES_POSTER;
import static com.tracker.util.Constants.FORMAT_HOURS;
import static com.tracker.util.Constants.FORMAT_LONG;
import static com.tracker.util.Constants.ID_SERIE;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private List<SerieResponse.Serie> mSeriesFavs;
    private static Context mContext;

    public FavoritesAdapter(Context mContext, List<SerieResponse.Serie> serie) {
        this.mSeriesFavs = serie;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public FavoritesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return FavoritesAdapter.ViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoritesAdapter.ViewHolder holder, final int position) {
        holder.bindTo(mSeriesFavs.get(position));
    }

    @Override
    public int getItemCount() {
        if (mSeriesFavs != null) {
            return mSeriesFavs.size();
        }
        return 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView favPoster;
        TextView favName;
        TextView favStatus;
        TextView favVistos;
        TextView next;
        TextView next2;
        TextView sinopsis;
        ProgressBar favProgress;
        int id;

        ConstraintLayout expandableView;
        Button arrowBtn;
        LinearLayout cardView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            favPoster = itemView.findViewById(R.id.posterBasic);
            favName = itemView.findViewById(R.id.serie_name);
            favStatus = itemView.findViewById(R.id.episode_fecha);
            favProgress = itemView.findViewById(R.id.progreso);
            favVistos = itemView.findViewById(R.id.vistos);
            next = itemView.findViewById(R.id.next_episode);
            next2 = itemView.findViewById(R.id.next_episode_2);
            sinopsis = itemView.findViewById(R.id.sinopsis);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                Bundle bundle = new Bundle();
                bundle.putInt(ID_SERIE, id);
                Navigation.findNavController(v).navigate(R.id.action_navigation_fav_to_navigation_series, bundle);
            });

            // Cardview interaction

            expandableView = itemView.findViewById(R.id.expandableView);
            arrowBtn = itemView.findViewById(R.id.arrowBtn);
            cardView = itemView.findViewById(R.id.cardView);

            arrowBtn.setOnClickListener(v -> {
                if (expandableView.getVisibility() == View.GONE) {
                    TransitionManager.beginDelayedTransition(cardView, new AutoTransition());
                    expandableView.setVisibility(View.VISIBLE);
                    arrowBtn.setBackgroundResource(R.drawable.arrow_collapse);
                } else {
                    TransitionManager.beginDelayedTransition(cardView, new AutoTransition());
                    expandableView.setVisibility(View.GONE);
                    arrowBtn.setBackgroundResource(R.drawable.arrow_expand);
                }
            });

            //
        }

        static FavoritesAdapter.ViewHolder create(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_series_following_card, parent, false);
            return new FavoritesAdapter.ViewHolder(view);
        }

        void bindTo(SerieResponse.Serie favSerie) {
            if (favSerie != null) {
                sortSeason(favSerie.seasons);
                id = favSerie.id;
                favName.setText(favSerie.name);
                Util.getImage(BASE_URL_IMAGES_POSTER + favSerie.posterPath, favPoster, mContext);
                favStatus.setText(favSerie.status);
                getEpisodesWatched(favSerie);
                next.setText(getLastEpisode(favSerie));
                next2.setText(getLastEpisode(favSerie));
                sinopsis.setText(getLastEpisodeSinopsis(favSerie, mContext));

            }
        }

        private void getEpisodesWatched(SerieResponse.Serie favSerie) {
            int totalEpisodes = favSerie.numberOfEpisodes;
            int vistos = countEpisodes(favSerie);
            int progress = 0;
            if (vistos > 0) {
                progress = (vistos * 100) / totalEpisodes;
            }
            favVistos.setText(mContext.getString(R.string.num_vistos, vistos, totalEpisodes));
            favProgress.setProgress(progress);
        }

        String getLastEpisodeSinopsis(SerieResponse.Serie serieFav, Context context) {
            Season.sortSeason(serieFav.seasons);
            for (Season s : serieFav.seasons) {
                for (Episode e : s.episodes) {
                    if (!e.visto) {
                        return e.overview;
                    }
                }
            }
            return String.format(context.getString(R.string.finished_date), Util.formatDateToString(serieFav.finishDate, FORMAT_LONG), Util.formatDateToString(serieFav.finishDate, FORMAT_HOURS));
        }

        String getLastEpisode(SerieResponse.Serie serieFav) {
            Season.sortSeason(serieFav.seasons);
            for (Season s : serieFav.seasons) {
                for (Episode e : s.episodes) {
                    if (!e.visto) {
                        return String.format(Locale.getDefault(), "%02dx%02d - %s", e.seasonNumber, e.episodeNumber, e.name);
                    }
                }
            }
            return mContext.getString(R.string.just_watch);
        }

        int countEpisodes(SerieResponse.Serie serieFav) {
            int cont = 0;
            for (Season s : serieFav.seasons) {
                for (Episode e : s.episodes) {
                    if (e.visto) {
                        cont++;
                    }
                }
            }
            return cont;
        }

        private void sortSeason(List<Season> seasons) {
            Collections.sort(seasons, (season1, season2) -> {
                String numSeason1 = String.valueOf(season1.seasonNumber);
                String numSeason2 = String.valueOf(season2.seasonNumber);
                if (numSeason1 != null && numSeason2 != null) {
                    return numSeason1.compareTo(numSeason2);
                } else {
                    String fecha1 = season1.airDate;
                    String fecha2 = season2.airDate;
                    return fecha1.compareTo(fecha2);
                }
            });
        }
    }
}
