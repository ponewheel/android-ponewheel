package net.kwatts.powtools.database;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class RideRow {

    public long rideId;
    public Date minEventDate;
    public Date maxEventDate;

    @Override public String toString() {
        return "("+rideId+")" + getMinuteDuration();
    }

    public long getMinuteDuration() {
        if (maxEventDate == null || minEventDate == null) {
            Timber.d( "minDate= " + minEventDate + " max=" + maxEventDate);
            return 0L;
        }
        return TimeUnit.MILLISECONDS.toMinutes(maxEventDate.getTime() - minEventDate.getTime());
    }

    public Date getMinDate() {
        return minEventDate;
    }
}
