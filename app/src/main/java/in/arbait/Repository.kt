package `in`.arbait

import `in`.arbait.database.User
import `in`.arbait.database.UserDatabase
import `in`.arbait.database.migration_1_2
import android.content.Context
import androidx.room.Room
import java.util.concurrent.Executors

private const val DATABASE_NAME = "user-database"

class UserRepository private constructor(context: Context) {

  private val database: UserDatabase = Room.databaseBuilder(
    context.applicationContext,
    UserDatabase::class.java,
    DATABASE_NAME
  ).addMigrations(migration_1_2).build()

  private val userDao = database.userDao()
  private val executor = Executors.newSingleThreadExecutor()

  suspend fun getUserLastByDate(): User? {
    return userDao.getUserLastByDate()
  }

  suspend fun getUserLastByDate (isConfirmed: Boolean): User? {
    return userDao.getUserLastByDate(isConfirmed)
  }

  fun updateUser (user: User) {
    executor.execute {
      userDao.updateUser(user)
    }
  }

  fun addUser (user: User) {
    executor.execute {
      userDao.addUser(user)
    }
  }

  fun deleteUser (user: User) {
    executor.execute {
      userDao.deleteUser(user)
    }
  }

  companion object {
    private var instance: UserRepository? = null

    fun initialize(context: Context) {
      if (instance == null) {
        instance = UserRepository(context)
      }
    }

    fun get(): UserRepository {
      return instance ?:
      throw IllegalStateException("UserRepository must be initialized")
    }
  }
}