package com.demo.toutiao.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.paging.PagingSource;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.paging.LimitOffsetPagingSource;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class NewsDao_Impl implements NewsDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfClearCategory;

  private final EntityUpsertionAdapter<NewsEntity> __upsertionAdapterOfNewsEntity;

  public NewsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfClearCategory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM news WHERE category = ?";
        return _query;
      }
    };
    this.__upsertionAdapterOfNewsEntity = new EntityUpsertionAdapter<NewsEntity>(new EntityInsertionAdapter<NewsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `news` (`id`,`category`,`title`,`description`,`source`,`imageUrl`,`originalUrl`,`publishTime`,`layoutType`,`position`,`cachedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NewsEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getCategory());
        statement.bindString(3, entity.getTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDescription());
        }
        if (entity.getSource() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getSource());
        }
        if (entity.getImageUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getImageUrl());
        }
        if (entity.getOriginalUrl() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getOriginalUrl());
        }
        if (entity.getPublishTime() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getPublishTime());
        }
        statement.bindString(9, entity.getLayoutType());
        statement.bindLong(10, entity.getPosition());
        statement.bindLong(11, entity.getCachedAt());
      }
    }, new EntityDeletionOrUpdateAdapter<NewsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `news` SET `id` = ?,`category` = ?,`title` = ?,`description` = ?,`source` = ?,`imageUrl` = ?,`originalUrl` = ?,`publishTime` = ?,`layoutType` = ?,`position` = ?,`cachedAt` = ? WHERE `id` = ? AND `category` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NewsEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getCategory());
        statement.bindString(3, entity.getTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDescription());
        }
        if (entity.getSource() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getSource());
        }
        if (entity.getImageUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getImageUrl());
        }
        if (entity.getOriginalUrl() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getOriginalUrl());
        }
        if (entity.getPublishTime() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getPublishTime());
        }
        statement.bindString(9, entity.getLayoutType());
        statement.bindLong(10, entity.getPosition());
        statement.bindLong(11, entity.getCachedAt());
        statement.bindString(12, entity.getId());
        statement.bindString(13, entity.getCategory());
      }
    });
  }

  @Override
  public Object clearCategory(final String cat, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearCategory.acquire();
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
          __preparedStmtOfClearCategory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<NewsEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfNewsEntity.upsert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public PagingSource<Integer, NewsEntity> pagingSource(final String cat) {
    final String _sql = "SELECT * FROM news WHERE category = ? ORDER BY position ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, cat);
    return new LimitOffsetPagingSource<NewsEntity>(_statement, __db, "news") {
      @Override
      @NonNull
      protected List<NewsEntity> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(cursor, "category");
        final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(cursor, "title");
        final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(cursor, "description");
        final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(cursor, "source");
        final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(cursor, "imageUrl");
        final int _cursorIndexOfOriginalUrl = CursorUtil.getColumnIndexOrThrow(cursor, "originalUrl");
        final int _cursorIndexOfPublishTime = CursorUtil.getColumnIndexOrThrow(cursor, "publishTime");
        final int _cursorIndexOfLayoutType = CursorUtil.getColumnIndexOrThrow(cursor, "layoutType");
        final int _cursorIndexOfPosition = CursorUtil.getColumnIndexOrThrow(cursor, "position");
        final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(cursor, "cachedAt");
        final List<NewsEntity> _result = new ArrayList<NewsEntity>(cursor.getCount());
        while (cursor.moveToNext()) {
          final NewsEntity _item;
          final String _tmpId;
          _tmpId = cursor.getString(_cursorIndexOfId);
          final String _tmpCategory;
          _tmpCategory = cursor.getString(_cursorIndexOfCategory);
          final String _tmpTitle;
          _tmpTitle = cursor.getString(_cursorIndexOfTitle);
          final String _tmpDescription;
          if (cursor.isNull(_cursorIndexOfDescription)) {
            _tmpDescription = null;
          } else {
            _tmpDescription = cursor.getString(_cursorIndexOfDescription);
          }
          final String _tmpSource;
          if (cursor.isNull(_cursorIndexOfSource)) {
            _tmpSource = null;
          } else {
            _tmpSource = cursor.getString(_cursorIndexOfSource);
          }
          final String _tmpImageUrl;
          if (cursor.isNull(_cursorIndexOfImageUrl)) {
            _tmpImageUrl = null;
          } else {
            _tmpImageUrl = cursor.getString(_cursorIndexOfImageUrl);
          }
          final String _tmpOriginalUrl;
          if (cursor.isNull(_cursorIndexOfOriginalUrl)) {
            _tmpOriginalUrl = null;
          } else {
            _tmpOriginalUrl = cursor.getString(_cursorIndexOfOriginalUrl);
          }
          final String _tmpPublishTime;
          if (cursor.isNull(_cursorIndexOfPublishTime)) {
            _tmpPublishTime = null;
          } else {
            _tmpPublishTime = cursor.getString(_cursorIndexOfPublishTime);
          }
          final String _tmpLayoutType;
          _tmpLayoutType = cursor.getString(_cursorIndexOfLayoutType);
          final int _tmpPosition;
          _tmpPosition = cursor.getInt(_cursorIndexOfPosition);
          final long _tmpCachedAt;
          _tmpCachedAt = cursor.getLong(_cursorIndexOfCachedAt);
          _item = new NewsEntity(_tmpId,_tmpCategory,_tmpTitle,_tmpDescription,_tmpSource,_tmpImageUrl,_tmpOriginalUrl,_tmpPublishTime,_tmpLayoutType,_tmpPosition,_tmpCachedAt);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public Object loadPage(final String cat, final int limit, final int offset,
      final Continuation<? super List<NewsEntity>> $completion) {
    final String _sql = "SELECT * FROM news WHERE category = ? ORDER BY position ASC LIMIT ? OFFSET ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, cat);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    _argIndex = 3;
    _statement.bindLong(_argIndex, offset);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<NewsEntity>>() {
      @Override
      @NonNull
      public List<NewsEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfOriginalUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "originalUrl");
          final int _cursorIndexOfPublishTime = CursorUtil.getColumnIndexOrThrow(_cursor, "publishTime");
          final int _cursorIndexOfLayoutType = CursorUtil.getColumnIndexOrThrow(_cursor, "layoutType");
          final int _cursorIndexOfPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "position");
          final int _cursorIndexOfCachedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "cachedAt");
          final List<NewsEntity> _result = new ArrayList<NewsEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NewsEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpSource;
            if (_cursor.isNull(_cursorIndexOfSource)) {
              _tmpSource = null;
            } else {
              _tmpSource = _cursor.getString(_cursorIndexOfSource);
            }
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpOriginalUrl;
            if (_cursor.isNull(_cursorIndexOfOriginalUrl)) {
              _tmpOriginalUrl = null;
            } else {
              _tmpOriginalUrl = _cursor.getString(_cursorIndexOfOriginalUrl);
            }
            final String _tmpPublishTime;
            if (_cursor.isNull(_cursorIndexOfPublishTime)) {
              _tmpPublishTime = null;
            } else {
              _tmpPublishTime = _cursor.getString(_cursorIndexOfPublishTime);
            }
            final String _tmpLayoutType;
            _tmpLayoutType = _cursor.getString(_cursorIndexOfLayoutType);
            final int _tmpPosition;
            _tmpPosition = _cursor.getInt(_cursorIndexOfPosition);
            final long _tmpCachedAt;
            _tmpCachedAt = _cursor.getLong(_cursorIndexOfCachedAt);
            _item = new NewsEntity(_tmpId,_tmpCategory,_tmpTitle,_tmpDescription,_tmpSource,_tmpImageUrl,_tmpOriginalUrl,_tmpPublishTime,_tmpLayoutType,_tmpPosition,_tmpCachedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object count(final String cat, final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM news WHERE category = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, cat);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
