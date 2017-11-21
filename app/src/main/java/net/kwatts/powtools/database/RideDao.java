package net.kwatts.powtools.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface RideDao {
    @Query("SELECT * FROM Ride")
    List<Ride> getAll();

    @Insert
    void insert(Ride ride);
}
