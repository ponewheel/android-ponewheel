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
            + "min(moment1.date) as minEventDate, "
            + "max(moment2.date) as maxEventDate "
            + "FROM Ride "
            + "INNER JOIN MOMENT as moment1 on moment1.ride_id = ride.id "
            + "INNER JOIN MOMENT as moment2 on moment2.ride_id = ride.id "
            + "GROUP BY rideId"
    )
    List<RideRow> getRideRowList();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Ride ride);
}
