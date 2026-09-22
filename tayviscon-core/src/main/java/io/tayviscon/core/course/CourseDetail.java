package io.tayviscon.core.course;

import java.util.List;

/** Полное описание курса: оглавление, путь IDE и необязательный логотип. */
public record CourseDetail(
    Course course, List<OutlineNode> outline, String idePath, LogoAsset logo) {
  /** Возвращает заголовки оглавления в порядке каталога. */
  public List<String> sections() {
    return outline.stream().map(OutlineNode::title).toList();
  }
}
