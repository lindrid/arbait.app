package `in`.arbait.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(entities = [User::class], version = 3)
@TypeConverters(UserTypeConverters::class)
abstract class UserDatabase: RoomDatabase() {
  abstract fun userDao(): UserDao
}

val migration_1_2 = object: Migration(1, 2) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("ALTER TABLE User ADD COLUMN callReceived TINYINT NOT NULL DEFAULT 0")
  }
}

val migration_2_3 = object: Migration(2, 3) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("ALTER TABLE User ADD COLUMN headerWasPressed TINYINT NOT NULL DEFAULT 0")
  }
}