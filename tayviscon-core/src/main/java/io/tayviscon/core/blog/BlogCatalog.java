package io.tayviscon.core.blog;

import java.util.List;
import java.util.Optional;

/** Каталог статей knowledge-base: дерево, лента, тело и ассеты. */
public interface BlogCatalog {
  /** Группы и опубликованные статьи для сайдбара. */
  List<BlogTreeGroup> tree();

  /** До двух последних постов по дате. */
  List<BlogPost> latest();

  /** Статья по пути или пусто, если путь неизвестен / черновик. */
  Optional<BlogArticle> article(String path);

  /** Ассет опубликованной статьи или пусто. */
  Optional<BlogAsset> asset(String path);
}
