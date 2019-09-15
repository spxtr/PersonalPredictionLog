package spxtr.predictionapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes")
    LiveData<List<Note>> getAll();

    @Query("SELECT * FROM notes WHERE prediction_uid = :uid")
    LiveData<List<Note>> get(int uid);

    @Insert
    void insert(Note... notes);
}
