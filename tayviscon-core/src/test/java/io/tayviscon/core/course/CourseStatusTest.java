package io.tayviscon.core.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CourseStatusTest {
  @Test
  void mapsNewAndLegacyValues() {
    assertEquals(CourseStatus.IN_PROGRESS, CourseStatus.fromCatalog("in_progress"));
    assertEquals(CourseStatus.PUBLISHED, CourseStatus.fromCatalog("published"));
    assertEquals(CourseStatus.FINISHED, CourseStatus.fromCatalog("finished"));
    assertEquals(CourseStatus.IN_PROGRESS, CourseStatus.fromCatalog("stub"));
    assertEquals(CourseStatus.PUBLISHED, CourseStatus.fromCatalog("content"));
  }

  @Test
  void badgesAreRussian() {
    assertEquals("в работе", CourseStatus.IN_PROGRESS.badge());
    assertEquals("доступен", CourseStatus.PUBLISHED.badge());
    assertEquals("завершён", CourseStatus.FINISHED.badge());
  }

  @Test
  void rejectsUnknown() {
    assertThrows(IllegalArgumentException.class, () -> CourseStatus.fromCatalog("stopped"));
  }
}
