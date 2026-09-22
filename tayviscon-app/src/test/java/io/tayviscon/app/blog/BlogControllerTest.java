package io.tayviscon.app.blog;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.tayviscon.core.blog.BlogArticle;
import io.tayviscon.core.blog.BlogAsset;
import io.tayviscon.core.blog.BlogCatalog;
import io.tayviscon.core.blog.BlogPost;
import io.tayviscon.core.blog.BlogTreeGroup;
import io.tayviscon.core.blog.BlogTreeItem;
import io.tayviscon.core.blog.BlogUnavailableException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BlogController.class)
@AutoConfigureMockMvc(addFilters = false)
class BlogControllerTest {

  @Autowired MockMvc mvc;

  @MockitoBean BlogCatalog catalog;

  @Test
  void treeJson() throws Exception {
    when(catalog.tree())
        .thenReturn(
            List.of(
                new BlogTreeGroup(
                    "systems",
                    List.of(
                        new BlogTreeItem(
                            "Надежные системы", "systems/online-code-execution")))));
    mvc.perform(get("/api/blog/tree"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groups[0].name").value("systems"))
        .andExpect(jsonPath("$.groups[0].articles[0].path").value("systems/online-code-execution"));
  }

  @Test
  void latestTwo() throws Exception {
    when(catalog.latest())
        .thenReturn(
            List.of(
                new BlogPost("A", "sa", "2026-09-20", "systems/online-code-execution"),
                new BlogPost("B", "sb", "2026-09-18", "ai/agents")));
    mvc.perform(get("/api/blog/latest"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.posts.length()").value(2));
  }

  @Test
  void articleOk() throws Exception {
    when(catalog.article("systems/online-code-execution"))
        .thenReturn(
            Optional.of(
                new BlogArticle(
                    "Надежные системы",
                    "sum",
                    "2026-09-20",
                    "systems/online-code-execution",
                    "<p>hi</p>")));
    mvc.perform(get("/api/blog/articles/systems/online-code-execution"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Надежные системы"))
        .andExpect(jsonPath("$.html").value("<p>hi</p>"));
  }

  @Test
  void articleMissing404() throws Exception {
    when(catalog.article("nope")).thenReturn(Optional.empty());
    mvc.perform(get("/api/blog/articles/nope")).andExpect(status().isNotFound());
  }

  @Test
  void assetOk() throws Exception {
    when(catalog.asset("systems/online-code-execution/engine.excalidraw.svg"))
        .thenReturn(Optional.of(new BlogAsset("<svg/>".getBytes(UTF_8), "image/svg+xml")));
    mvc.perform(get("/api/blog/assets/systems/online-code-execution/engine.excalidraw.svg"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/svg+xml"));
  }

  @Test
  void unavailableIs503() throws Exception {
    when(catalog.tree()).thenThrow(new BlogUnavailableException("down", null));
    mvc.perform(get("/api/blog/tree"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("блог временно недоступен"));
  }
}
