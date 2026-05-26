package com.demo.toutiao.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RemoteKeysDao_Impl implements RemoteKeysDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  private final EntityUpsertionAdapter<RemoteKeysEntity> __upsertionAdapterOfRemoteKeysEntity;

  public RemoteKeysDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM remote_keys WHERE category = ?";
        return _query;
      }
    };
    this.__upsertionAdapterOfRemoteKeysEntity = new EntityUpsertionAdapter<RemoteKeysEntity>(new EntityInsertionAdapter<RemoteKeysEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `remote_keys` (`category`,`nextPage`,`lastUpdated`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RemoteKeysEntity entity) {
        statement.bindString(1, entity.getCategory());
        if (entity.getNextPage() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getNextPage());
        }
        statement.bindLong(3, entity.getLastUpdated());
      }
    }, new EntityDeletionOrUpdateAdapter<RemoteKeysEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `remote_keys` SET `category` = ?,`nextPage` = ?,`lastUpdated` = ? WHERE `category` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RemoteKeysEntity entity) {
        statement.bindString(1, entity.getCategory());
        if (entity.getNextPage() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getNextPage());
        }
        statement.bindLong(3, entity.getLastUpdated());
        statement.bindString(4, entity.getCategory());
      }
    });
  }

  @Override
  public Object delete(final String cat, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, cat);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final RemoteKeysEntity key, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfRemoteKeysEntity.upsert(key);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object get(final String cat, final Continuation<? super RemoteKeysEntity> $completion) {
    final String _sql = "SELECT * FROM remote_keys WHERE category = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, cat);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RemoteKeysEntity>() {
      @Override
      @Nullable
      public RemoteKeysEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfNextPage = CursorUtil.getColumnIndexOrThrow(_cursor, "nextPage");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final RemoteKeysEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final Integer _tmpNextPage;
            if (_cursor.isNull(_cursorIndexOfNextPage)) {
              _tmpNextPage = null;
            } else {
              _tmpNextPage = _cursor.getInt(_cursorIndexOfNextPage);
            }
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _result = new RemoteKeysEntity(_tmpCategory,_tmpNextPage,_tmpLastUpdated);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
