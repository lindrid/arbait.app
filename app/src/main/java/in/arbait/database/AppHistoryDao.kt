package `in`.arbait.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AppHistoryDao
{
  @Query("SELECT * FROM AppHistory WHERE appId = :appId")
  suspend fun get (appId: Int): AppHistory?

  @Update
  fun update (appHistory: AppHistory)

  @Insert
  fun add (appHistory: AppHistory)
}