package net.kwatts.powtools.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface MomentDao {
    @Query("SELECT * FROM Moment where :rideId = ride_id")
    List<Moment> getFromRide(long rideId);

    @Insert
    long insert(Moment moment);
}
