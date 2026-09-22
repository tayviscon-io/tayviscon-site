package io.tayviscon.app.course;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.tayviscon.core.course.Course;
import io.tayviscon.core.course.CourseDetail;
import io.tayviscon.core.course.CourseStatus;
import io.tayviscon.core.course.OutlineNode;
import java.util.List;

/** JSON-представление курса для публичного REST API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CourseApi(
    String id,
    String title,
    String summary,
    String status,
    String badge,
    String logoUrl,
    String fallbackLetter,
    List<String> sections,
    List<OutlineNode> outline,
    String idePath) {

  /** Собирает элемент списка без полей оглавления. */
  public static CourseApi from(Course course) {
    return new CourseApi(
        course.id(),
        course.title(),
        course.summary(),
        statusJson(course.status()),
        course.status().badge(),
        logoUrl(course),
        course.fallbackLetter(),
        null,
        null,
        null);
  }

  /** Собирает карточку курса с оглавлением и путём IDE. */
  public static CourseApi fromDetail(CourseDetail detail) {
    var course = detail.course();
    return new CourseApi(
        course.id(),
        course.title(),
        course.summary(),
        statusJson(course.status()),
        course.status().badge(),
        logoUrl(course),
        course.fallbackLetter(),
        detail.sections(),
        detail.outline(),
        detail.idePath());
  }

  private static String logoUrl(Course course) {
    return course.hasLogo() ? "/api/courses/" + course.id() + "/logo" : null;
  }

  private static String statusJson(CourseStatus status) {
    return switch (status) {
      case IN_PROGRESS -> "in_progress";
      case PUBLISHED -> "published";
      case FINISHED -> "finished";
    };
  }
}
