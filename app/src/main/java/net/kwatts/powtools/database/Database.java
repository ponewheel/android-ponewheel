package net.kwatts.powtools.database;

import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

import net.kwatts.powtools.database.daos.AttributeDao;
import net.kwatts.powtools.database.daos.MomentDao;
import net.kwatts.powtools.database.daos.RideDao;
import net.kwatts.powtools.database.entities.Attribute;
import net.kwatts.powtools.database.entities.Moment;
import net.kwatts.powtools.database.entities.Ride;

/**
 * To export
 * adb -d shell "run-as net.kwatts.powtools cat /data/data/net.kwatts.powtools/databases/database-name-pow" > database-name-pow.db
 */
@android.arch.persistence.room.Database(entities = {
        Ride.class,
        Moment.class,
        Attribute.class
}, version = 11)
@TypeConverters(DateConverter.class)
public abstract class Database extends RoomDatabase {
    public abstract RideDao rideDao();
    public abstract MomentDao momentDao();
    public abstract AttributeDao attributeDao();

}
