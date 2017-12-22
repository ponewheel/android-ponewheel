package net.kwatts.powtools.database.daos;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import net.kwatts.powtools.database.entities.Ride;

import java.util.List;

@Dao
public interface RideDao {
    @Query("SELECT * FROM Ride")
    List<Ride> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Ride ride);

    @Update
    public void updateRide(Ride ride);

    @Query("DELETE from RIDE where id = :rideId")
    void delete(long rideId);
}
