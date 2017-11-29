package net.kwatts.powtools.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import java.util.List;

@Dao
public interface RideDao {
    @Query("SELECT * FROM Ride")
    List<Ride> getAll();

    @Query("SELECT "
            + "ride.id as rideId, "
            + "min(momentMin.date) as minEventDate, "
            + "max(momentMax.date) as maxEventDate "
            + "FROM Ride "
            + "INNER JOIN MOMENT as momentMin on momentMin.ride_id = ride.id "
            + "INNER JOIN MOMENT as momentMax on momentMax.ride_id = ride.id "
            + "GROUP BY rideId"
    )
    List<RideRow> getRideRowList();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Ride ride);

    @Query("DELETE from RIDE where id = :rideId")
    void delete(long rideId);
}
