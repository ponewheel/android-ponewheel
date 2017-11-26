package net.kwatts.powtools.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.Nullable;
import java.util.Date;

@Entity(foreignKeys = @ForeignKey(entity = Ride.class,
        parentColumns = "id",
        childColumns = "ride_id"))
public class Moment {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "ride_id")
    public long rideId;

    @Nullable
    private double gpsLat;
    @Nullable
    private double gpsLong;

    private Date date;

    public Moment() {

    }
    public Moment(long rideId, Date date) {
        this.rideId = rideId;
        this.date = date;
    }


    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }


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
