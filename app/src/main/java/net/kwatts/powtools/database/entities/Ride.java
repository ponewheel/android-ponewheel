package net.kwatts.powtools.database.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

@Entity
public class Ride {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public Date start;
    public Date end;

    @Override public String toString() {
        return "Ride(" + Long.toString(id) + ")";
    }


    public long getMinuteDuration() {
        if (start == null || end == null) {
            Timber.d( "minDate= " + start + " max=" + end);
            return 0L;
        }
        return TimeUnit.MILLISECONDS.toMinutes(end.getTime() - start.getTime());
    }
}
