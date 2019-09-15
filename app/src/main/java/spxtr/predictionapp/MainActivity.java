package spxtr.predictionapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.opencsv.CSVWriter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private static PredictionDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = PredictionDatabase.getInstance(this);

        // Constrain probability input to integers 0 to 100 inclusive.
        EditText prob = findViewById(R.id.text_new_prediction_probability);
        prob.setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                try {
                    int input = Integer.parseInt(dest.toString() + source.toString());
                    if (input < 0 || input > 100) {
                        return "";
                    }
                } catch (NumberFormatException e) {
                    return "";
                }
                return null;
            }
        }});

        // This should be set in XML, but it doesn't have the same effect. Seems like a bug.
        ((EditText) findViewById(R.id.text_new_prediction)).setHorizontallyScrolling(false);
        ((EditText) findViewById(R.id.text_new_prediction)).setMaxLines(Integer.MAX_VALUE);

        ViewPager viewPager = findViewById(R.id.pager);
        ViewPredictionsPagerAdapter vppa = new ViewPredictionsPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(vppa);
        viewPager.setOffscreenPageLimit(2);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void onPredictClicked(View view) {
        boolean err = false;
        EditText text = findViewById(R.id.text_new_prediction);
        if (text.length() == 0) {
            text.setError(getResources().getString(R.string.error_no_prediction));
            err = true;
        }

        EditText prob = findViewById(R.id.text_new_prediction_probability);
        if (prob.length() == 0) {
            prob.setError(getResources().getString(R.string.error_no_probability));
            err = true;
        }

        Button dueDateBtn = findViewById(R.id.btn_set_due_date);
        OffsetDateTime dueDate = (OffsetDateTime) dueDateBtn.getTag();

        if (err) {
            return;
        }

        Prediction p = new Prediction();
        p.prediction = text.getText().toString();
        p.createDate = OffsetDateTime.now();
        p.dueDate = dueDate;
        p.probability = Integer.parseInt(prob.getText().toString());
        p.resolved = false;
        p.happened = false;
        new CreatePredictionTask().execute(p);

        prob.setText("");
        text.setText("");
        dueDateBtn.setTag(null);
        dueDateBtn.setText(getResources().getString(R.string.no_due_date));
        Toast.makeText(this, getResources().getString(R.string.prediction_added), Toast.LENGTH_SHORT).show();
    }

    public void onDueDateClicked(View view) {
        OffsetDateTime old = (OffsetDateTime) view.getTag();
        if (old == null) {
            old = OffsetDateTime.now();
        }
        final Button button = (Button) view;
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                OffsetDateTime chosen = OffsetDateTime.of(year, month + 1, day, 0, 0, 0, 0, OffsetDateTime.now().getOffset());
                button.setTag(chosen);
                // TODO: Format this better.
                button.setText(getResources().getString(R.string.due_on, chosen.format(DateTimeFormatter.ISO_LOCAL_DATE)));
            }
        }, old.getYear(), old.getMonthValue() - 1, old.getDayOfMonth()).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_statistics) {
            Intent intent = new Intent(this, StatisticsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_export) {
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_storage), Toast.LENGTH_SHORT).show();
                return true;
            }
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "predictions.csv");
            startActivityForResult(intent, 42);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 42 || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri[] uris = {data.getData()};
        new ExportCSVTask().execute(uris);
    }

    public static class CreatePredictionTask extends AsyncTask<Prediction, Void, Void> {
        @Override
        protected Void doInBackground(Prediction... params) {
            db.predictionDao().insert(params);
            return null;
        }
    }

    private class ExportCSVTask extends AsyncTask<Uri, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Uri... uris) {
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uris[0], "w");
                FileWriter fw = new FileWriter(pfd.getFileDescriptor());
                List<Prediction> predictions = db.predictionDao().getAllSync();
                CSVWriter writer = new CSVWriter(fw);
                for (Prediction p : predictions) {
                    ArrayList<String> entries = new ArrayList<>();
                    entries.add(p.createDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    entries.add(p.dueDate == null ? "" : p.dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    entries.add(p.prediction);
                    entries.add(p.probability + "%");
                    entries.add(p.resolved ? "true" : "false");
                    entries.add(p.resolved ? (p.happened ? "true" : "false") : "");
                    writer.writeNext(entries.toArray(new String[0]));
                }
                writer.close();
                fw.close();
                pfd.close();
            } catch (FileNotFoundException e) {
                Log.w("ExportCSVTask", e.toString());
                return false;
            } catch (IOException e) {
                Log.w("ExportCSVTask", e.toString());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.saved), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.save_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
