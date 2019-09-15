package spxtr.predictionapp;

import android.os.AsyncTask;

public class ResolvePredictionTask extends AsyncTask<Void, Void, Void> {

    PredictionDatabase db;
    int uid;
    boolean result;

    public ResolvePredictionTask(PredictionDatabase db, int uid, boolean result) {
        this.db = db;
        this.uid = uid;
        this.result = result;
    }

    @Override
    protected Void doInBackground(Void... in) {
        db.predictionDao().resolve(this.uid, this.result);
        return null;
    }
}
