package net.kwatts.powtools.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

@Entity(foreignKeys = @ForeignKey(entity = Ride.class,
        parentColumns = "ride_id",
        childColumns = "ride_id"))
public class Moment {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "moment_id")
    public int id;

    @ColumnInfo(name = "ride_id")
    public int rideId;
    private String speed;

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public String getSpeed() {
        return speed;
    }
}
