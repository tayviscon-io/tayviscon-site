package io.tayviscon.github;

/** Узел рекурсивного Git Tree: путь и признак каталога. */
public record GitHubTreeEntry(String path, boolean directory) {}
