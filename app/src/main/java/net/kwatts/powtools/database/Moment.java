package net.kwatts.powtools.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.Nullable;

@Entity(foreignKeys = @ForeignKey(entity = Ride.class,
        parentColumns = "ride_id",
        childColumns = "ride_id"))
public class Moment {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "moment_id")
    public int id;

    @ColumnInfo(name = "ride_id")
    public int rideId;

    @Nullable
    private double gpsLat;
    @Nullable
    private double gpsLong;


    public void setGpsLat(double gpsLat) {
        this.gpsLat = gpsLat;
    }

    public double getGpsLat() {
        return gpsLat;
    }

    public void setGpsLong(double gpsLong) {
        this.gpsLong = gpsLong;
    }

    public double getGpsLong() {
        return gpsLong;
    }
}
