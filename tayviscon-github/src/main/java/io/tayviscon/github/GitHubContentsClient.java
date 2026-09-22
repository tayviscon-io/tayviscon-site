package io.tayviscon.github;

import java.util.List;
import java.util.Optional;

/** Читает файлы и дерево репозитория через GitHub Contents / Git Trees API. */
public interface GitHubContentsClient {
  /** Возвращает UTF-8 текст файла {@code path} или бросает исключение, если файла нет. */
  String getFile(String path);

  /** Возвращает сырые байты файла {@code path}. */
  byte[] getRaw(String path);

  /** Рекурсивное дерево ветки (по умолчанию пусто, если клиент не умеет). */
  default List<GitHubTreeEntry> listTree() {
    return List.of();
  }

  /** Возвращает UTF-8 текст {@code path} или пустое значение, если GitHub ответил 404. */
  default Optional<String> getFileIfPresent(String path) {
    try {
      return Optional.of(getFile(path));
    } catch (RuntimeException e) {
      var message = e.getMessage();
      if (message != null
          && (message.contains("HTTP 404") || message.startsWith("Not found:"))) {
        return Optional.empty();
      }
      throw e;
    }
  }
}
