package net.kwatts.powtools.database.daos;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import net.kwatts.powtools.database.entities.Attribute;

import java.util.List;

@Dao
public abstract class AttributeDao {
    @Query("SELECT * FROM Attribute where :momentId = moment_id")
    public abstract List<Attribute> getFromMoment(long momentId);

    @Query("SELECT * "
            + "FROM Attribute "
            + "where "
            + ":keyName = \"key\" and "
            + ":momentId = moment_id")
    public abstract Attribute getFromMomentAndKey(long momentId, String keyName);

    @Insert
    public abstract long insert(Attribute attribute);

    @Insert
    public abstract void insertAll(List<Attribute> attributes);

    @Query("SELECT distinct(\"key\") " +
            "FROM Attribute " +
            "INNER JOIN Moment on moment_id = moment.id " +
            "where ride_id = :rideId ")
    public abstract List<String> getDistinctKeysFromRide(long rideId);
}
