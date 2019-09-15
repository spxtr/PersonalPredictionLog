package spxtr.predictionapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.OffsetDateTime;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "prediction_uid")
    public int prediction_uid;

    @ColumnInfo(name = "note")
    public String note;

    @ColumnInfo(name = "create_date")
    public OffsetDateTime createDate;
}