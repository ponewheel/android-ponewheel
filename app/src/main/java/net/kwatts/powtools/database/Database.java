package net.kwatts.powtools.database;

import android.arch.persistence.room.RoomDatabase;

@android.arch.persistence.room.Database(entities = {
        Moment.class,
        Ride.class
}, version = 1)
public abstract class Database extends RoomDatabase {
    public abstract RideDao rideDao();
    public abstract MomentDao momentDao();

}
