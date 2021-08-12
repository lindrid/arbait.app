package `in`.arbait.database

import androidx.room.*

@Dao
interface UserDao {
  @Query("SELECT * FROM user")
  fun getUsers(): List<User>

  @Query("SELECT * FROM user WHERE isConfirmed = (:isConfirmed)")
  fun getUser (isConfirmed: Boolean): User

  @Update
  fun updateUser (user: User)

  @Insert
  fun addUser (user: User)

  @Delete
  fun deleteUser (user: User): Int
}