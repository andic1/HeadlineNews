package com.demo.toutiao.data.repo;

import com.demo.toutiao.data.api.NewsApi;
import com.demo.toutiao.data.db.AppDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class NewsRepository_Factory implements Factory<NewsRepository> {
  private final Provider<NewsApi> apiProvider;

  private final Provider<AppDatabase> dbProvider;

  public NewsRepository_Factory(Provider<NewsApi> apiProvider, Provider<AppDatabase> dbProvider) {
    this.apiProvider = apiProvider;
    this.dbProvider = dbProvider;
  }

  @Override
  public NewsRepository get() {
    return newInstance(apiProvider.get(), dbProvider.get());
  }

  public static NewsRepository_Factory create(Provider<NewsApi> apiProvider,
      Provider<AppDatabase> dbProvider) {
    return new NewsRepository_Factory(apiProvider, dbProvider);
  }

  public static NewsRepository newInstance(NewsApi api, AppDatabase db) {
    return new NewsRepository(api, db);
  }
}
