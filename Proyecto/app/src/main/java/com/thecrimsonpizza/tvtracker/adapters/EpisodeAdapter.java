package com.thecrimsonpizza.tvtracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

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
import java.util.Locale;

import static com.thecrimsonpizza.tvtracker.util.Constants.BASE_URL_IMAGES_POSTER;
import static com.thecrimsonpizza.tvtracker.util.Constants.FORMAT_LONG;

/**
 * Adapter from the RecyclerView that hosts the Episodes info
 */
public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private final Context mContext;
    private final List<Episode> mEpisodes;
    private final SerieResponse.Serie mSerie;
    private final List<SerieResponse.Serie> mFavs;
    private final int mSeasonPosition;

    public EpisodeAdapter(Context mContext, List<Episode> episodes, SerieResponse.Serie serie,
                          List<SerieResponse.Serie> favs, int seasonPos) {
        this.mFavs = favs;
        this.mSerie = serie;
        this.mEpisodes = episodes;
        this.mContext = mContext;
        this.mSeasonPosition = seasonPos;
    }

    @NonNull
    @Override
    public EpisodeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return EpisodeAdapter.ViewHolder.create(parent);
    }


    @Override
    public void onBindViewHolder(@NonNull EpisodeAdapter.ViewHolder holder, final int position) {
        holder.bindTo(mEpisodes.get(position), mSerie, mFavs, mSeasonPosition, mContext);
    }

    @Override
    public int getItemCount() {
        if (mEpisodes != null) {
            return mEpisodes.size();
        }
        return 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView episodeName;
        private final TextView episodeNameExpandable;
        private final TextView episodeDate;
        private final TextView episodeOverview;
        private final TextView episodeTime;
        private final ImageView episodeBackdrop;
        private final MaterialCheckBox watchedCheck;

        private final ConstraintLayout expandableView;
        private final Button arrowBtn;
        private final LinearLayout cardView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            episodeBackdrop = itemView.findViewById(R.id.image_episode);
            episodeName = itemView.findViewById(R.id.episode_name);
            episodeDate = itemView.findViewById(R.id.episode_fecha);
            episodeOverview = itemView.findViewById(R.id.episode_sinopsis);
            episodeTime = itemView.findViewById(R.id.episode_time);
            watchedCheck = itemView.findViewById(R.id.checkbox_watched);
            episodeNameExpandable = itemView.findViewById(R.id.next_episode);

            // CARDVIEW INTERACTION
            expandableView = itemView.findViewById(R.id.expandableViewEpi);
            arrowBtn = itemView.findViewById(R.id.arrowBtnEpi);
            cardView = itemView.findViewById(R.id.cardview);
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
        }

        static EpisodeAdapter.ViewHolder create(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_episodes, parent, false);
            return new EpisodeAdapter.ViewHolder(view);
        }

        void bindTo(Episode episode, SerieResponse.Serie serie, List<SerieResponse.Serie> favs, int mSeasonPos, Context c) {
            if (episode != null) {
                setWatchedCheck(episode, serie, favs, mSeasonPos);

                episodeName.setText(episode.name);
                episodeNameExpandable.setText(episode.name);
                episodeDate.setText(Util.convertStringDateFormat(episode.airDate, FORMAT_LONG));
                episodeOverview.setText(Util.checkNull(episode.overview, c));
                Util.getImage(BASE_URL_IMAGES_POSTER + episode.stillPath, episodeBackdrop, c);

                // RUNTIME
                if (!serie.episodeRunTime.isEmpty()) {
                    episodeTime.setText(getMinutos(serie.episodeRunTime.get(0)));
                } else {
                    episodeTime.setText(getMinutos(0));
                }
            }
        }

        /**
         * Checks if the episode is watched and sets it true or false the CheckBox
         *
         * @param episode   episode of the season
         * @param serie     serie loaded in the SerieFragment
         * @param favs      list of series in the following list
         * @param seasonPos position of the season in the season list
         */
        private void setWatchedCheck(Episode episode, SerieResponse.Serie serie, List<SerieResponse.Serie> favs, int seasonPos) {
            if (serie.added) {
                watchedCheck.setVisibility(View.VISIBLE);
                watchedCheck.setChecked(episode.visto);
                watchedCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        if (!episode.visto) {
                            watchEpisode(serie, favs, getAdapterPosition(), seasonPos);
                        }
                    } else {
                        if (episode.visto) {
                            unwatchEpisode(serie, favs, getAdapterPosition(), seasonPos);
                        }
                    }
                });
            }
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
         * Checks if the episodes are watched so the season is finished or not
         *
         * @param serie we need its seasons and episodes
         * @return true or false
         */
        private boolean checkEpisodesFinished(SerieResponse.Serie serie) {
            for (Season s : serie.seasons) {
                for (Episode e : s.episodes) {
                    if (!e.visto) return false;
                }
            }
            return true;
        }

        /**
         * Set as watched the episode in the RecyclerView and then in the Database
         *
         * @param serie      serie loaded in the SerieFragment
         * @param favs       list of series in the following list
         * @param episodePos episode position in the RecyclerView
         * @param seasonPos  position of the season in the season list
         */
        private void watchEpisode(SerieResponse.Serie serie, List<SerieResponse.Serie> favs, int episodePos, int seasonPos) {
            int favPosition = serie.getPosition(favs);
            if (favPosition != -1) {
                favs.get(favPosition)
                        .seasons.get(seasonPos)
                        .episodes.get(episodePos)
                        .visto = true;
                favs.get(favPosition)
                        .seasons.get(seasonPos)
                        .episodes.get(episodePos)
                        .watchedDate = new Date();

                if (checkEpisodesFinished(favs.get(favPosition))) {
                    favs.get(favPosition)
                            .seasons.get(seasonPos).visto = true;
                    favs.get(favPosition)
                            .seasons.get(seasonPos).watchedDate = new Date();
                } else {
                    favs.get(favPosition)
                            .seasons.get(seasonPos).visto = false;
                    favs.get(favPosition)
                            .seasons.get(seasonPos).watchedDate = null;
                }

                if (checkSeasonFinished(favs.get(favPosition))) {
                    favs.get(favPosition).finished = true;
                    favs.get(favPosition).finishDate = new Date();
                } else {
                    favs.get(favPosition).finished = false;
                    favs.get(favPosition).finishDate = null;
                }

                FirebaseDb.getInstance(FirebaseAuth.getInstance().getCurrentUser()).setSeriesFav(favs);
            }
        }

        /**
         * Set as unwatched the episode in the RecyclerView and the in the Database
         *
         * @param serie      serie loaded in the SerieFragment
         * @param favs       list of series in the following list
         * @param episodePos episode position in the RecyclerView
         * @param seasonPos  position of the season in the season list
         */
        private void unwatchEpisode(SerieResponse.Serie serie, List<SerieResponse.Serie> favs, int episodePos, int seasonPos) {
            int favPosition = serie.getPosition(favs);
            if (favPosition != -1) {
                favs.get(favPosition)
                        .seasons.get(seasonPos)
                        .episodes.get(episodePos)
                        .visto = false;
                favs.get(favPosition)
                        .seasons.get(seasonPos)
                        .episodes.get(episodePos)
                        .watchedDate = null;

                favs.get(favPosition)
                        .seasons.get(seasonPos).visto = false;
                favs.get(favPosition)
                        .seasons.get(seasonPos).watchedDate = null;

                favs.get(favPosition).finished = false;
                favs.get(favPosition).finishDate = null;

                FirebaseDb.getInstance(FirebaseAuth.getInstance().getCurrentUser()).setSeriesFav(favs);
            }
        }

        /**
         * Turns minutes to mm:ss format
         *
         * @param minutes minutes to convert format
         * @return a string with the mm:ss format
         */
        private String getMinutos(int minutes) {
            return String.format(Locale.getDefault(), Constants.FORMAT_MINUTES, minutes, 0);
        }
    }
}