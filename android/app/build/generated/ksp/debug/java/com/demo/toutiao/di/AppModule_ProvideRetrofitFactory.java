package com.demo.toutiao.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;
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
public final class AppModule_ProvideRetrofitFactory implements Factory<Retrofit> {
  private final Provider<OkHttpClient> clientProvider;

  private final Provider<Json> jsonProvider;

  public AppModule_ProvideRetrofitFactory(Provider<OkHttpClient> clientProvider,
      Provider<Json> jsonProvider) {
    this.clientProvider = clientProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public Retrofit get() {
    return provideRetrofit(clientProvider.get(), jsonProvider.get());
  }

  public static AppModule_ProvideRetrofitFactory create(Provider<OkHttpClient> clientProvider,
      Provider<Json> jsonProvider) {
    return new AppModule_ProvideRetrofitFactory(clientProvider, jsonProvider);
  }

  public static Retrofit provideRetrofit(OkHttpClient client, Json json) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideRetrofit(client, json));
  }
}
