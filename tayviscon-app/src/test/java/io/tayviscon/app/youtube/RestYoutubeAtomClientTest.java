package io.tayviscon.app.youtube;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestYoutubeAtomClientTest {
  @Test
  void fetchesAtomWithChannelIdQuery() {
    var builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(
            requestTo(
                "https://www.youtube.com/feeds/videos.xml?channel_id=UCZhzhQbtFdKturwa_7FGIMA"))
        .andRespond(withSuccess("<feed/>", MediaType.APPLICATION_ATOM_XML));
    var client = new RestYoutubeAtomClient(builder.build(), "UCZhzhQbtFdKturwa_7FGIMA");
    assertEquals("<feed/>", client.fetchAtom());
    server.verify();
  }
}
