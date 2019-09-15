package spxtr.predictionapp;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        final LineAndPointFormatter seriesFormat = new LineAndPointFormatter(Color.BLACK, Color.BLACK, null, null);
        seriesFormat.getLinePaint().setStrokeWidth(PixelUtils.dpToPix(2));
        final XYPlot plot = findViewById(R.id.plot);
        plot.setDomainBoundaries(0, 100, BoundaryMode.FIXED);
        plot.setRangeBoundaries(0, 1, BoundaryMode.FIXED);
        Number[] xr = {0, 100};
        Number[] yr = {0, 1};
        final XYSeries perfectSeries = new SimpleXYSeries(Arrays.asList(xr), Arrays.asList(yr), "perfection");
        final LineAndPointFormatter perfectSeriesFormatter = new LineAndPointFormatter(Color.DKGRAY, null, null, null);
        perfectSeriesFormatter.getLinePaint().setPathEffect(new DashPathEffect(new float[]{
                PixelUtils.dpToPix(20),
                PixelUtils.dpToPix(15)}, 0));

        final PredictionViewModel viewModel = ViewModelProviders.of(this).get(PredictionViewModel.class);
        viewModel.getAllPredictions().observe(this, new Observer<List<Prediction>>() {
            @Override
            public void onChanged(@NonNull final List<Prediction> predictions) {
                // Bin predictions and outcomes. For a low number of predictions, don't bother
                // trying to fit then into 10% bins.
                List<List<Integer>> ps = new ArrayList<>(10);
                List<List<Integer>> os = new ArrayList<>(10);
                for (int i = 0; i < 10; i++) {
                    ps.add(new ArrayList<Integer>());
                    os.add(new ArrayList<Integer>());
                }

                if (predictions.size() < 100) {
                    Collections.sort(predictions, new Comparator<Prediction>() {
                        @Override
                        public int compare(Prediction p1, Prediction p2) {
                            return p1.probability - p2.probability;
                        }
                    });
                }

                double score = 0.0;
                for (int i = 0; i < predictions.size(); i++) {
                    Prediction p = predictions.get(i);
                    if (!p.resolved) {
                        continue;
                    }
                    int bin;
                    if (predictions.size() < 100) {
                        bin = i / 10;
                    } else {
                        bin = p.probability / 10;
                    }
                    if (bin == 10) {
                        bin = 9;
                    }
                    ps.get(bin).add(p.probability);
                    os.get(bin).add(p.happened ? 1 : 0);
                    score += (p.happened ? 1 : 0) - (p.probability / 100.0);
                }

                // Average the bins to get points.
                List<Double> histXVals = new ArrayList<>();
                List<Double> histYVals = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    if (ps.get(i).size() != 0) {
                        int sumX = 0;
                        int sumY = 0;
                        for (Integer p : ps.get(i)) {
                            sumX += p;
                        }
                        for (Integer o : os.get(i)) {
                            sumY += o;
                        }
                        histXVals.add((double) sumX / ps.get(i).size());
                        histYVals.add((double) sumY / os.get(i).size());
                    }
                }
                XYSeries series = new SimpleXYSeries(histXVals, histYVals, "calibration");
                plot.clear();
                plot.addSeries(perfectSeries, perfectSeriesFormatter);
                plot.addSeries(series, seriesFormat);
                plot.redraw();
                ((TextView) findViewById(R.id.text_score)).setText(getResources().getString(R.string.score, String.format(getResources().getConfiguration().getLocales().get(0), "%.2f", score)));
            }
        });
    }
}
