package net.kwatts.powtools.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Ride {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="ride_id")
    public int id;


}
