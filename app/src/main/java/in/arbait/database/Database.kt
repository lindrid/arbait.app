package `in`.arbait.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [User::class, EnrollingPermission::class], version = 9)
@TypeConverters(MyTypeConverters::class)
abstract class Database: RoomDatabase() {
  abstract fun userDao(): UserDao
  abstract fun enrollingPermissionDao(): EnrollingPermissionDao
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

val migration_3_4 = object: Migration(3, 4) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("CREATE TABLE User_Backup (phone TEXT NOT NULL, " +
        "callReceived TINYINT NOT NULL, " +
        "isConfirmed TINYINT NOT NULL, " +
        "login TINYINT NOT NULL, " +
        "headerWasPressed TINYINT NOT NULL DEFAULT 0, " +
        "createdAt INTEGER NOT NULL, " +
        "PRIMARY KEY (phone))")
    database.execSQL("INSERT INTO User_Backup SELECT phone, callReceived, isConfirmed," +
        "login, headerWasPressed, createdAt FROM User")
    database.execSQL("DROP TABLE User")
    database.execSQL("ALTER TABLE User_Backup RENAME to User")
  }
}

val migration_4_5 = object: Migration(4, 5) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("ALTER TABLE User ADD COLUMN id INTEGER NOT NULL DEFAULT 0")
  }
}

val migration_5_6 = object: Migration(5, 6) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("ALTER TABLE User ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
  }
}

val migration_6_7 = object: Migration(6, 7) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("ALTER TABLE User ADD COLUMN isItRegistration TINYINT NOT NULL DEFAULT 0")
  }
}

val migration_7_8 = object: Migration(7, 8) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("ALTER TABLE User ADD COLUMN notificationsOff TINYINT NOT NULL DEFAULT 0")
    database.execSQL("ALTER TABLE User ADD COLUMN soundOff TINYINT NOT NULL DEFAULT 0")
  }
}

val migration_8_9 = object : Migration(8, 9){
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL(
      "CREATE TABLE IF NOT EXISTS `EnrollingPermission` (" +
            "`userId` INTEGER NOT NULL DEFAULT 0," +
            "`clickCountWithinOneMin` INTEGER NOT NULL DEFAULT 0," +
            "`enableClickTime` INTEGER NOT NULL DEFAULT 0," +
            "`lastClickTime` INTEGER NOT NULL DEFAULT 0," +
            "PRIMARY KEY(`userId`)" +
          ")"
    )
  }
}