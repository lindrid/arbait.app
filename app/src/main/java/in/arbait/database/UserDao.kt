package `in`.arbait.database

import androidx.room.*

@Dao
interface UserDao {

  @Query("SELECT * FROM user ORDER BY createdAt DESC limit 1")
  suspend fun getUserLastByDate (): User?

  @Query("SELECT * FROM user WHERE isConfirmed = (:isConfirmed) ORDER BY createdAt DESC limit 1")
  suspend fun getUserLastByDate (isConfirmed: Boolean): User?

  @Update
  fun updateUser (user: User)

  @Insert
  fun addUser (user: User)

  @Delete
  fun deleteUser (user: User): Int
}