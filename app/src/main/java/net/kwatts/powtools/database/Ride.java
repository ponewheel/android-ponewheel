package net.kwatts.powtools.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Ride {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @Override public String toString() {
        return "id";
    }

}
