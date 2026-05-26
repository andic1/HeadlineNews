package com.demo.toutiao.di;

import com.demo.toutiao.data.api.NewsApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class AppModule_ProvideNewsApiFactory implements Factory<NewsApi> {
  private final Provider<Retrofit> retrofitProvider;

  public AppModule_ProvideNewsApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public NewsApi get() {
    return provideNewsApi(retrofitProvider.get());
  }

  public static AppModule_ProvideNewsApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new AppModule_ProvideNewsApiFactory(retrofitProvider);
  }

  public static NewsApi provideNewsApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideNewsApi(retrofit));
  }
}
