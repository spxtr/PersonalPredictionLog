package spxtr.predictionapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EditPredictionActivity extends AppCompatActivity {
    private PredictionDatabase pdb;
    private NoteDatabase ndb;
    private int uid;

    private static String formatDate(OffsetDateTime now, OffsetDateTime date) {
        return DateUtils.getRelativeTimeSpanString(date.toEpochSecond() * 1000, now.toEpochSecond() * 1000, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_prediction);

        uid = getIntent().getIntExtra("prediction_uid", 0);
        final PredictionViewModel pvm = ViewModelProviders.of(this).get(PredictionViewModel.class);
        pdb = PredictionDatabase.getInstance(this);
        pvm.prediction(uid).observe(this, new Observer<Prediction>() {
            @Override
            public void onChanged(@NonNull final Prediction p) {
                refreshPrediction(p);
            }
        });

        final NoteViewModel nvm = ViewModelProviders.of(this).get(NoteViewModel.class);
        ndb = NoteDatabase.getInstance(this);
        nvm.setDb(ndb);
        nvm.getNotes(uid).observe(this, new Observer<List<Note>>() {
            @Override
            public void onChanged(@NonNull final List<Note> notes) {
                ArrayList<Note> ns = new ArrayList<>(notes);
                Comparator<Note> compareNotes = new Comparator<Note>() {
                    @Override
                    public int compare(Note n1, Note n2) {
                        return n1.createDate.compareTo(n2.createDate);
                    }
                };
                Collections.sort(ns, compareNotes);
                refreshNotes(ns);
            }
        });

        ((EditText) findViewById(R.id.note_text)).setHorizontallyScrolling(false);
        ((EditText) findViewById(R.id.note_text)).setMaxLines(Integer.MAX_VALUE);
    }

    private void refreshPrediction(final Prediction p) {
        if (p.dueDate == null && !p.resolved) {
            findViewById(R.id.btn_set_due_date).setVisibility(View.VISIBLE);
            final Context ctx = this;
            findViewById(R.id.btn_set_due_date).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    OffsetDateTime now = OffsetDateTime.now();
                    new DatePickerDialog(ctx, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                            p.dueDate = OffsetDateTime.of(year, month + 1, day, 0, 0, 0, 0, OffsetDateTime.now().getOffset());
                            new UpdatePredictionTask(pdb).execute(p);
                        }
                    }, now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth()).show();
                }
            });
        } else {
            findViewById(R.id.btn_set_due_date).setVisibility(View.GONE);
        }
        if (p.resolved) {
            findViewById(R.id.btn_unresolve).setVisibility(View.VISIBLE);
            findViewById(R.id.btn_unresolve).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    p.resolved = false;
                    new UpdatePredictionTask(pdb).execute(p);
                }
            });
            findViewById(R.id.btn_yes).setVisibility(View.GONE);
            findViewById(R.id.btn_no).setVisibility(View.GONE);
        } else {
            findViewById(R.id.btn_unresolve).setVisibility(View.GONE);
            findViewById(R.id.btn_yes).setVisibility(View.VISIBLE);
            findViewById(R.id.btn_yes).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    p.resolved = true;
                    p.happened = true;
                    new UpdatePredictionTask(pdb).execute(p);
                }
            });
            findViewById(R.id.btn_no).setVisibility(View.VISIBLE);
            findViewById(R.id.btn_no).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    p.resolved = true;
                    p.happened = false;
                    new UpdatePredictionTask(pdb).execute(p);
                }
            });
        }

        ((TextView) findViewById(R.id.text_prediction)).setText(p.prediction);
        ((TextView) findViewById(R.id.text_probability)).setText(getResources().getString(R.string.probability, p.probability));
        if (p.dueDate == null) {
            ((TextView) findViewById(R.id.text_create_due_date)).setText(getResources().getString(R.string.created, formatDate(OffsetDateTime.now(), p.createDate)));
        } else {
            ((TextView) findViewById(R.id.text_create_due_date)).setText(getResources().getString(R.string.created_and_due, formatDate(OffsetDateTime.now(), p.createDate), formatDate(OffsetDateTime.now(), p.dueDate)));
        }
        if (p.resolved) {
            if (p.happened) {
                ((TextView) findViewById(R.id.text_resolution)).setText(getResources().getString(R.string.text_true));
            } else {
                ((TextView) findViewById(R.id.text_resolution)).setText(getResources().getString(R.string.text_false));
            }
        } else {
            ((TextView) findViewById(R.id.text_resolution)).setText(getResources().getString(R.string.unresolved));
        }
    }

    private void refreshNotes(List<Note> ns) {
        Collections.sort(ns, new Comparator<Note>() {
            @Override
            public int compare(Note n1, Note n2) {
                return n2.createDate.compareTo(n1.createDate);
            }
        });
        findViewById(R.id.progress_bar).setVisibility(View.INVISIBLE);
        LinearLayout ll = findViewById(R.id.table_notes);
        ll.removeAllViews();

        LayoutInflater inflater = getLayoutInflater();
        for (Note n : ns) {
            View v = inflater.inflate(R.layout.view_note, ll, false);

            ((TextView) v.findViewById(R.id.text_create_date)).setText(formatDate(OffsetDateTime.now(), n.createDate));
            ((TextView) v.findViewById(R.id.note)).setText(n.note);

            ll.addView(v);
        }
    }

    public void createNote(View view) {
        String noteText = ((EditText) findViewById(R.id.note_text)).getText().toString();
        if (noteText.length() == 0) {
            ((EditText) findViewById(R.id.note_text)).setError("You gotta add a note tho!");
            return;
        }
        Note n = new Note();
        n.createDate = OffsetDateTime.now();
        n.prediction_uid = uid;
        n.note = noteText;
        new CreateNoteTask(ndb).execute(n);
        ((EditText) findViewById(R.id.note_text)).setText("");
    }

    private static class CreateNoteTask extends AsyncTask<Note, Void, Void> {
        private NoteDatabase db;

        private CreateNoteTask(NoteDatabase db) {
            this.db = db;
        }

        @Override
        public Void doInBackground(Note... params) {
            db.noteDao().insert(params);
            return null;
        }
    }

    private static class UpdatePredictionTask extends AsyncTask<Prediction, Void, Void> {
        private PredictionDatabase db;

        private UpdatePredictionTask(PredictionDatabase db) {
            this.db = db;
        }

        @Override
        public Void doInBackground(Prediction... params) {
            db.predictionDao().update(params);
            return null;
        }
    }
}
