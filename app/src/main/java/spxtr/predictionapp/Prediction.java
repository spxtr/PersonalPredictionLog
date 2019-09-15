package spxtr.predictionapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.OffsetDateTime;

@Entity(tableName = "predictions")
public class Prediction {

    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "prediction")
    public String prediction;

    @ColumnInfo(name = "probability")
    public int probability;

    @ColumnInfo(name = "due_date")
    public OffsetDateTime dueDate;

    @ColumnInfo(name = "create_date")
    public OffsetDateTime createDate;

    @ColumnInfo(name = "resolved")
    public boolean resolved;

    @ColumnInfo(name = "happened")
    public boolean happened;
}
