package io.tayviscon.core.course;

import java.util.List;
import java.util.Optional;

/** Каталог курсов, карточек и логотипов только для чтения. */
public interface CourseCatalog {
  /** Возвращает все курсы текущего каталога. */
  List<Course> list();

  /** Возвращает курс с идентификатором {@code id}, если он есть. */
  Optional<CourseDetail> findById(String id);

  /** Возвращает логотип курса {@code id}, если он есть. */
  default Optional<LogoAsset> logo(String id) {
    return findById(id).map(CourseDetail::logo);
  }
}
