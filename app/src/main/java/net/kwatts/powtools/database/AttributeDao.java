package net.kwatts.powtools.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import java.util.List;

@Dao
public interface AttributeDao {
    @Query("SELECT * FROM Attribute where :momentId = moment_id")
    List<Attribute> getFromMoment(int momentId);

    @Insert
    void insert(Attribute attribute);
}
