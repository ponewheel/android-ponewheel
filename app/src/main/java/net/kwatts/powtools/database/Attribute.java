package net.kwatts.powtools.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.Nullable;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(foreignKeys = @ForeignKey(entity = Moment.class,
        parentColumns = "id",
        childColumns = "moment_id",
        onDelete = CASCADE))
public class Attribute {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "attribute_id")
    public long id;


    @ColumnInfo(name = "moment_id")
    public long momentId;

    @Nullable
    private String value;
//    @Nullable
//    private String uuid;
//    @Nullable
//    private String uiName;
    @Nullable
    private String key;

    public void setMomentId(long momentId) {
        this.momentId = momentId;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
