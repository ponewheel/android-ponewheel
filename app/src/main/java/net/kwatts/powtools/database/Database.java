package net.kwatts.powtools.database;

import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

@android.arch.persistence.room.Database(entities = {
        Ride.class,
        Moment.class,
        Attribute.class
}, version = 8)
@TypeConverters(DateConverter.class)
public abstract class Database extends RoomDatabase {
    public abstract RideDao rideDao();
    public abstract MomentDao momentDao();
    public abstract AttributeDao attributeDao();

}
