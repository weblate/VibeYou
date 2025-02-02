package app.suhasdissa.mellowmusic.backend.database.dao

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface RawDao {
    @RawQuery
    fun raw(supportSQLiteQuery: SupportSQLiteQuery): Int
}
