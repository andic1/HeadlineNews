package com.demo.toutiao.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile NewsDao _newsDao;

  private volatile RemoteKeysDao _remoteKeysDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `news` (`id` TEXT NOT NULL, `category` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `source` TEXT, `imageUrl` TEXT, `originalUrl` TEXT, `publishTime` TEXT, `layoutType` TEXT NOT NULL, `position` INTEGER NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`id`, `category`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `remote_keys` (`category` TEXT NOT NULL, `nextPage` INTEGER, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`category`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '2429aec14ee2c379e63ad82529254e5c')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `news`");
        db.execSQL("DROP TABLE IF EXISTS `remote_keys`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsNews = new HashMap<String, TableInfo.Column>(11);
        _columnsNews.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNews.put("category", new TableInfo.Column("category", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNews.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNews.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNews.put("source", new TableInfo.Column("source", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNews.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNews.put("originalUrl", new TableInfo.Column("originalUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNews.put("publishTime", new TableInfo.Column("publishTime", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNews.put("layoutType", new TableInfo.Column("layoutType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNews.put("position", new TableInfo.Column("position", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNews.put("cachedAt", new TableInfo.Column("cachedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNews = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesNews = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoNews = new TableInfo("news", _columnsNews, _foreignKeysNews, _indicesNews);
        final TableInfo _existingNews = TableInfo.read(db, "news");
        if (!_infoNews.equals(_existingNews)) {
          return new RoomOpenHelper.ValidationResult(false, "news(com.demo.toutiao.data.db.NewsEntity).\n"
                  + " Expected:\n" + _infoNews + "\n"
                  + " Found:\n" + _existingNews);
        }
        final HashMap<String, TableInfo.Column> _columnsRemoteKeys = new HashMap<String, TableInfo.Column>(3);
        _columnsRemoteKeys.put("category", new TableInfo.Column("category", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemoteKeys.put("nextPage", new TableInfo.Column("nextPage", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRemoteKeys.put("lastUpdated", new TableInfo.Column("lastUpdated", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRemoteKeys = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRemoteKeys = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRemoteKeys = new TableInfo("remote_keys", _columnsRemoteKeys, _foreignKeysRemoteKeys, _indicesRemoteKeys);
        final TableInfo _existingRemoteKeys = TableInfo.read(db, "remote_keys");
        if (!_infoRemoteKeys.equals(_existingRemoteKeys)) {
          return new RoomOpenHelper.ValidationResult(false, "remote_keys(com.demo.toutiao.data.db.RemoteKeysEntity).\n"
                  + " Expected:\n" + _infoRemoteKeys + "\n"
                  + " Found:\n" + _existingRemoteKeys);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "2429aec14ee2c379e63ad82529254e5c", "87c5bd3255c6350eaeb9559300412d0f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "news","remote_keys");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `news`");
      _db.execSQL("DELETE FROM `remote_keys`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(NewsDao.class, NewsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RemoteKeysDao.class, RemoteKeysDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public NewsDao newsDao() {
    if (_newsDao != null) {
      return _newsDao;
    } else {
      synchronized(this) {
        if(_newsDao == null) {
          _newsDao = new NewsDao_Impl(this);
        }
        return _newsDao;
      }
    }
  }

  @Override
  public RemoteKeysDao remoteKeysDao() {
    if (_remoteKeysDao != null) {
      return _remoteKeysDao;
    } else {
      synchronized(this) {
        if(_remoteKeysDao == null) {
          _remoteKeysDao = new RemoteKeysDao_Impl(this);
        }
        return _remoteKeysDao;
      }
    }
  }
}
