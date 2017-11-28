package net.kwatts.powtools.database;

import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

/**
 * To export
 * adb -d shell "run-as net.kwatts.powtools cat /data/data/net.kwatts.powtools/databases/database-name-pow" > database-name-pow.db
 */
@android.arch.persistence.room.Database(entities = {
        Ride.class,
        Moment.class,
        Attribute.class
}, version = 9)
@TypeConverters(DateConverter.class)
public abstract class Database extends RoomDatabase {
    public abstract RideDao rideDao();
    public abstract MomentDao momentDao();
    public abstract AttributeDao attributeDao();

}
