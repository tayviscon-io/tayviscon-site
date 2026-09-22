package io.tayviscon.app.youtube;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.tayviscon.core.youtube.YoutubeFeed;
import io.tayviscon.core.youtube.YoutubeUnavailableException;
import io.tayviscon.core.youtube.YoutubeVideo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(YoutubeController.class)
@AutoConfigureMockMvc(addFilters = false)
class YoutubeControllerTest {

  @Autowired MockMvc mvc;

  @MockitoBean YoutubeFeed feed;

  @Test
  void listsLatestVideos() throws Exception {
    when(feed.latest())
        .thenReturn(
            List.of(
                new YoutubeVideo(
                    "lhGamS89atg",
                    "Tayviscon: Рождение из хаоса",
                    "https://www.youtube.com/watch?v=lhGamS89atg",
                    "2024-08-05",
                    "https://i.ytimg.com/vi/lhGamS89atg/mqdefault.jpg")));
    mvc.perform(get("/api/youtube/latest"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.videos[0].id").value("lhGamS89atg"))
        .andExpect(jsonPath("$.videos[0].title").value("Tayviscon: Рождение из хаоса"))
        .andExpect(
            jsonPath("$.videos[0].url").value("https://www.youtube.com/watch?v=lhGamS89atg"))
        .andExpect(jsonPath("$.videos[0].published").value("2024-08-05"))
        .andExpect(
            jsonPath("$.videos[0].thumbnailUrl")
                .value("https://i.ytimg.com/vi/lhGamS89atg/mqdefault.jpg"));
  }

  @Test
  void unavailableIs503() throws Exception {
    when(feed.latest()).thenThrow(new YoutubeUnavailableException("down", null));
    mvc.perform(get("/api/youtube/latest"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("канал временно недоступен"));
  }
}
