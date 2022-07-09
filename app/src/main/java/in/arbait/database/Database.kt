package `in`.arbait.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [User::class, EnrollingPermission::class, AppHistory::class], version = 15)
@TypeConverters(MyTypeConverters::class)
abstract class Database: RoomDatabase() {
  abstract fun userDao(): UserDao
  abstract fun enrollingPermissionDao(): EnrollingPermissionDao
  abstract fun appHistoryDao(): AppHistoryDao
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

val migration_8_9 = object : Migration(8, 9) {
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

val migration_9_10 = object : Migration(9, 10) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL(
      "CREATE TABLE `EnrollingPermissionBackup` (" +
          "`userId` INTEGER NOT NULL DEFAULT 0," +
          "`changeStateCount` INTEGER NOT NULL DEFAULT 0," +
          "`enableClickTime` INTEGER NOT NULL DEFAULT 0," +
          "`lastClickTime` INTEGER NOT NULL DEFAULT 0," +
          "PRIMARY KEY(`userId`)" +
          ")"
    )
    database.execSQL("INSERT INTO EnrollingPermissionBackup SELECT userId, " +
        "clickCountWithinOneMin, enableClickTime, lastClickTime FROM EnrollingPermission")
    database.execSQL("DROP TABLE EnrollingPermission")
    database.execSQL("ALTER TABLE EnrollingPermissionBackup RENAME to EnrollingPermission")

    database.execSQL("ALTER TABLE EnrollingPermission " +
        "ADD COLUMN lastState TINYINT NOT NULL DEFAULT 0")
  }
}

val migration_10_11 = object: Migration(10, 11) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("ALTER TABLE User ADD COLUMN sberFio TEXT NOT NULL DEFAULT ''")
    database.execSQL("ALTER TABLE User ADD COLUMN anotherBank4Digits TEXT NOT NULL DEFAULT ''")
  }
}

val migration_11_12 = object: Migration(11, 12) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("ALTER TABLE User ADD COLUMN rulesShowDatetime INT8 NOT NULL DEFAULT 0")
  }
}

val migration_12_13 = object : Migration(12, 13) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL(
  "CREATE TABLE IF NOT EXISTS `AppHistory` (" +
        "`appId` INTEGER NOT NULL DEFAULT 0," +
        "`enrollTime` INT8 NOT NULL DEFAULT 0," +
        "`refuseTime` INT8 NOT NULL DEFAULT 0," +
        "`consequences` INTEGER NOT NULL DEFAULT 0," +
        "PRIMARY KEY(`appId`)" +
      ")"
    )
  }
}

val migration_13_14 = object: Migration(13, 14) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("ALTER TABLE User ADD COLUMN endOfBannDatetime INT8 NOT NULL DEFAULT 0")
  }
}

val migration_14_15 = object: Migration(14, 15) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("ALTER TABLE User ADD COLUMN needToShowInfo TINYINT NOT NULL DEFAULT 1")
  }
}