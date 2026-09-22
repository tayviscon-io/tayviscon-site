package io.tayviscon.core.youtube;

import java.util.List;

/** Последние видео канала Tayviscon на YouTube. */
public interface YoutubeFeed {
  /** Возвращает самые свежие видео, сначала новые. */
  List<YoutubeVideo> latest();
}
