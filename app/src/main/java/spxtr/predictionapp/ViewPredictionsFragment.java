package spxtr.predictionapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class ViewPredictionsFragment extends Fragment {

    public static final int ROLE_DUE = 0;
    public static final int ROLE_UNRESOLVED = 1;
    public static final int ROLE_ALL = 2;

    private PredictionViewModel pvm;
    private int role;
    private Comparator<Prediction> compareDueThenCreateDates = new Comparator<Prediction>() {
        @Override
        public int compare(Prediction p1, Prediction p2) {
            OffsetDateTime dueDate1 = p1.dueDate == null ? p1.createDate : p1.dueDate;
            OffsetDateTime dueDate2 = p2.dueDate == null ? p2.createDate : p2.dueDate;
            int v1 = dueDate1.compareTo(dueDate2);
            if (v1 == 0) {
                return p1.createDate.compareTo(p2.createDate);
            }
            return v1;
        }
    };
    private Comparator<Prediction> compareCreateDates = new Comparator<Prediction>() {
        @Override
        public int compare(Prediction p1, Prediction p2) {
            return p2.createDate.compareTo(p1.createDate);
        }
    };

    private static String formatDate(OffsetDateTime now, OffsetDateTime date) {
        return DateUtils.getRelativeTimeSpanString(date.toEpochSecond() * 1000, now.toEpochSecond() * 1000, DateUtils.DAY_IN_MILLIS, 0).toString();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            role = args.getInt("role");
        } else {
            role = ROLE_DUE;
            Log.wtf(getClass().getSimpleName(), "No args passed to onCreate?!");
        }

        pvm = ViewModelProviders.of(this).get(PredictionViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_view_predictions, container, false);
        pvm.getAllPredictions().observe(getViewLifecycleOwner(), new Observer<List<Prediction>>() {
            @Override
            public void onChanged(@NonNull final List<Prediction> predictions) {
                refreshPredictions(root, predictions);
            }
        });
        return root;
    }

    private void refreshPredictions(View root, List<Prediction> predictions) {
        root.findViewById(R.id.progress_bar).setVisibility(View.INVISIBLE);
        if (role == ROLE_ALL) {
            Collections.sort(predictions, compareCreateDates);
        } else {
            Collections.sort(predictions, compareDueThenCreateDates);
        }

        LinearLayout ll = root.findViewById(R.id.table_predictions);
        ll.removeAllViews();

        // First filter depending on which tab we are.
        OffsetDateTime now = OffsetDateTime.now();
        ArrayList<Prediction> filteredPredictions = new ArrayList<>();
        for (final Prediction p : predictions) {
            if (role == ROLE_ALL) {
                filteredPredictions.add(p);
            }
            if (p.resolved) {
                continue;
            }
            if (role == ROLE_UNRESOLVED) {
                filteredPredictions.add(p);
            }
            if (p.dueDate != null && now.isBefore(p.dueDate)) {
                continue;
            }
            if (role == ROLE_DUE) {
                filteredPredictions.add(p);
            }
        }

        if (predictions.size() > 0) {
            ((TextView) root.findViewById(R.id.text_nothing_here)).setText(getResources().getString(R.string.nothing_here));
        } else {
            ((TextView) root.findViewById(R.id.text_nothing_here)).setText(getResources().getString(R.string.no_predictions));
        }

        // Add or remove the empty state TextView.
        if (filteredPredictions.size() > 0) {
            root.findViewById(R.id.text_nothing_here).setVisibility(View.GONE);
        } else {
            root.findViewById(R.id.text_nothing_here).setVisibility(View.VISIBLE);
        }

        // Now add the filtered predictions.
        LayoutInflater inflater = getLayoutInflater();
        for (final Prediction p : filteredPredictions) {
            View v = inflater.inflate(R.layout.view_prediction, ll, false);
            ((TextView) v.findViewById(R.id.text_prediction)).setText(p.prediction);
            ((TextView) v.findViewById(R.id.text_probability)).setText(v.getResources().getString(R.string.probability, p.probability));
            if (role == ROLE_ALL) {
                if (p.dueDate == null) {
                    ((TextView) v.findViewById(R.id.text_create_due_date)).setText(v.getResources().getString(R.string.created, formatDate(now, p.createDate)));
                } else {
                    ((TextView) v.findViewById(R.id.text_create_due_date)).setText(v.getResources().getString(R.string.created_and_due, formatDate(now, p.createDate), formatDate(now, p.dueDate)));
                }
            } else {
                if (p.dueDate == null) {
                    v.findViewById(R.id.text_create_due_date).setVisibility(View.GONE);
                } else {
                    ((TextView) v.findViewById(R.id.text_create_due_date)).setText(formatDate(now, p.dueDate));
                }
                v.findViewById(R.id.text_resolution).setVisibility(View.GONE);
            }
            if (p.resolved) {
                if (p.happened) {
                    ((TextView) v.findViewById(R.id.text_resolution)).setText(v.getResources().getString(R.string.text_true));
                } else {
                    ((TextView) v.findViewById(R.id.text_resolution)).setText(v.getResources().getString(R.string.text_false));
                }
            } else if (role == ROLE_ALL) {
                ((TextView) v.findViewById(R.id.text_resolution)).setText(v.getResources().getString(R.string.unresolved));
            }
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), EditPredictionActivity.class);
                    intent.putExtra("prediction_uid", p.uid);
                    startActivity(intent);
                }
            });
            ll.addView(v);
        }
    }
}
