package net.kwatts.powtools.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import net.kwatts.powtools.model.OWDevice;

import java.util.Date;

@Entity
public class Ride {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name="ride_id")
    public int id;
    private Date date;


    public Ride(Date date) {
        this.date = date;
        System.out.println("date = " + date);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return OWDevice.SIMPLE_DATE_FORMAT.format(date);
    }
}
