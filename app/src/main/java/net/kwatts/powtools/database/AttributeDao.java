package net.kwatts.powtools.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import java.util.List;

@Dao
public interface AttributeDao {
    @Query("SELECT * FROM Attribute where :momentId = moment_id")
    List<Attribute> getFromMoment(long momentId);

    @Query("SELECT * "
            + "FROM Attribute "
            + "where "
            + ":keyName = \"key\" and "
            + ":momentId = moment_id")
    Attribute getFromMomentAndKey(long momentId, String keyName);

    @Insert
    void insert(Attribute attribute);
}
