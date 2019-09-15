package spxtr.predictionapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PredictionDao {
    @Query("SELECT * FROM predictions")
    LiveData<List<Prediction>> getAll();

    @Query("SELECT * FROM predictions")
    List<Prediction> getAllSync();

    @Query("SELECT * FROM predictions WHERE uid = :uid")
    LiveData<Prediction> get(int uid);

    @Update
    void update(Prediction... predictions);

    @Insert
    void insert(Prediction... predictions);

    @Query("UPDATE predictions SET resolved = 1, happened = :happened WHERE uid = :uid")
    void resolve(int uid, boolean happened);
}
