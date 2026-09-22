package io.tayviscon.app.youtube;

/** Клиент, который скачивает Atom XML канала. */
public interface YoutubeAtomClient {
  /** Возвращает тело Atom XML или бросает исключение, если лента недоступна. */
  String fetchAtom();
}
