package `in`.arbait.database

import androidx.room.*

@Dao
interface EnrollingPermissionDao
{
  @Query("SELECT * FROM EnrollingPermission WHERE userId = :userId")
  suspend fun get (userId: Int): EnrollingPermission?

  @Update
  fun update (permission: EnrollingPermission)

  @Insert
  fun add (permission: EnrollingPermission)

  @Delete
  fun deleteUser (permission: EnrollingPermission): Int
}