package com.demo.toutiao.ui.home;

import com.demo.toutiao.data.repo.AiRepository;
import com.demo.toutiao.data.repo.NewsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<NewsRepository> repoProvider;

  private final Provider<AiRepository> aiRepoProvider;

  public HomeViewModel_Factory(Provider<NewsRepository> repoProvider,
      Provider<AiRepository> aiRepoProvider) {
    this.repoProvider = repoProvider;
    this.aiRepoProvider = aiRepoProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(repoProvider.get(), aiRepoProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<NewsRepository> repoProvider,
      Provider<AiRepository> aiRepoProvider) {
    return new HomeViewModel_Factory(repoProvider, aiRepoProvider);
  }

  public static HomeViewModel newInstance(NewsRepository repo, AiRepository aiRepo) {
    return new HomeViewModel(repo, aiRepo);
  }
}
