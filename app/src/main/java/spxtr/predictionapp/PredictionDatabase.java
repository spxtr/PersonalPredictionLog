package spxtr.predictionapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {Prediction.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class PredictionDatabase extends RoomDatabase {
    private static PredictionDatabase instance;

    public static PredictionDatabase getInstance(Context ctx) {
        if (instance == null) {
            instance = Room.databaseBuilder(ctx.getApplicationContext(), PredictionDatabase.class, "predictions").build();
        }
        return instance;
    }

    public abstract PredictionDao predictionDao();
}