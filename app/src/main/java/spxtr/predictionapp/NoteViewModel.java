package spxtr.predictionapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class NoteViewModel extends ViewModel {
    public LiveData<List<Note>> notes;
    private boolean dbSet = false;
    private NoteDatabase ndb;

    public void setDb(NoteDatabase db) {
        if (dbSet) {
            return;
        }
        notes = db.noteDao().getAll();
        this.ndb = db;
    }

    public LiveData<List<Note>> getNotes(int uid) {
        return ndb.noteDao().get(uid);
    }
}