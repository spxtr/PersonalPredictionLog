package spxtr.predictionapp;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class PredictionViewModel extends AndroidViewModel {

    private PredictionDatabase db;
    private LiveData<List<Prediction>> predictions;

    public PredictionViewModel(Application application) {
        super(application);
        this.db = PredictionDatabase.getInstance(application.getApplicationContext());
        predictions = db.predictionDao().getAll();
    }

    public LiveData<Prediction> prediction(int uid) {
        return db.predictionDao().get(uid);
    }

    public LiveData<List<Prediction>> getAllPredictions() {
        return predictions;
    }
}
