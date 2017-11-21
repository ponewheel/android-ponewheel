package net.kwatts.powtools.database;

import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

@android.arch.persistence.room.Database(entities = {
        Moment.class,
        Ride.class
}, version = 2)
@TypeConverters(DateConverter.class)
public abstract class Database extends RoomDatabase {
    public abstract RideDao rideDao();
    public abstract MomentDao momentDao();

}
