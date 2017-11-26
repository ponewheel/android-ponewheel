package net.kwatts.powtools.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.Nullable;
import java.util.Date;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(foreignKeys = @ForeignKey(entity = Ride.class,
        parentColumns = "id",
        childColumns = "ride_id",
        onDelete = CASCADE))
public class Moment {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "ride_id")
    public long rideId;


    @Nullable private String gpsLat;
    @Nullable private String gpsLong;

    private Date date;

    /**
     * @deprecated Not for public use, use @link(net.kwatts.powtools.database.Moment#Moment(long, java.util.Date))
     */
    public Moment() {}

    @Ignore
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
        this.gpsLat = Double.toString(gpsLat);
    }
    public Double getGpsLatDouble() {
        return gpsLat != null ? Double.parseDouble(gpsLat) : 0.0;
    }

    public void setGpsLong(double gpsLong) {
        this.gpsLong = Double.toString(gpsLong);
    }
    public Double getGpsLongDouble() {
        return gpsLong != null ? Double.parseDouble(gpsLong) : 0.0;
    }

    public String getGpsLat() {
        return gpsLat;
    }
    public String getGpsLong() {
        return gpsLong;
    }

    public void setGpsLat(@Nullable String gpsLat) {
        this.gpsLat = gpsLat;
    }

    public void setGpsLong(@Nullable String gpsLong) {
        this.gpsLong = gpsLong;
    }

}
