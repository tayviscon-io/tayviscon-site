package io.tayviscon.app.course;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.tayviscon.core.course.CatalogUnavailableException;
import io.tayviscon.core.course.Course;
import io.tayviscon.core.course.CourseCatalog;
import io.tayviscon.core.course.CourseDetail;
import io.tayviscon.core.course.CourseStatus;
import io.tayviscon.core.course.LogoAsset;
import io.tayviscon.core.course.OutlineNode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

  @Autowired MockMvc mvc;

  @MockitoBean CourseCatalog catalog;

  @Test
  void listsCourses() throws Exception {
    when(catalog.list())
        .thenReturn(
            List.of(
                new Course(
                    "sql",
                    "Yet Another SQL Course",
                    "sum",
                    CourseStatus.PUBLISHED,
                    "S",
                    true)));
    mvc.perform(get("/api/courses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.courses[0].badge").value("доступен"))
        .andExpect(jsonPath("$.courses[0].status").value("published"))
        .andExpect(jsonPath("$.courses[0].logoUrl").value("/api/courses/sql/logo"));
  }

  @Test
  void omitsLogoUrlWhenCourseHasNoLogo() throws Exception {
    when(catalog.list())
        .thenReturn(
            List.of(
                new Course(
                    "java",
                    "Yet Another Java Course",
                    "sum",
                    CourseStatus.IN_PROGRESS,
                    "J",
                    false)));
    mvc.perform(get("/api/courses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.courses[0].logoUrl").doesNotExist());
  }

  @Test
  void courseDetailHasSectionsAndIdePath() throws Exception {
    when(catalog.findById("sql"))
        .thenReturn(
            Optional.of(
                new CourseDetail(
                    new Course(
                        "sql",
                        "Yet Another SQL Course",
                        "sum",
                        CourseStatus.PUBLISHED,
                        "S",
                        true),
                    OutlineNode.leaves(List.of("Введение")),
                    "courses/sql/",
                    new LogoAsset("<svg/>".getBytes(), "image/svg+xml"))));
    mvc.perform(get("/api/courses/sql"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0]").value("Введение"))
        .andExpect(jsonPath("$.outline[0].title").value("Введение"))
        .andExpect(jsonPath("$.idePath").value("courses/sql/"))
        .andExpect(jsonPath("$.logoUrl").value("/api/courses/sql/logo"));
  }

  @Test
  void servesLogoBytes() throws Exception {
    var png = new byte[] {1, 2, 3};
    when(catalog.logo("sql")).thenReturn(Optional.of(new LogoAsset(png, "image/png")));
    mvc.perform(get("/api/courses/sql/logo"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(header().string("Content-Disposition", containsString("attachment")))
        .andExpect(content().bytes(png));
  }

  @Test
  void svgLogoIsSandboxed() throws Exception {
    var svg = "<svg></svg>".getBytes();
    when(catalog.logo("sql")).thenReturn(Optional.of(new LogoAsset(svg, "image/svg+xml")));
    mvc.perform(get("/api/courses/sql/logo"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Security-Policy", containsString("sandbox")))
        .andExpect(header().string("Content-Disposition", containsString("attachment")));
  }

  @Test
  void missingLogoIs404() throws Exception {
    when(catalog.logo("sql")).thenReturn(Optional.empty());
    mvc.perform(get("/api/courses/sql/logo")).andExpect(status().isNotFound());
  }

  @Test
  void missingCourseIs404() throws Exception {
    when(catalog.findById("nope")).thenReturn(Optional.empty());
    mvc.perform(get("/api/courses/nope")).andExpect(status().isNotFound());
  }

  @Test
  void unavailableIs503() throws Exception {
    when(catalog.list()).thenThrow(new CatalogUnavailableException("down", null));
    mvc.perform(get("/api/courses"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("каталог временно недоступен"));
  }
}
