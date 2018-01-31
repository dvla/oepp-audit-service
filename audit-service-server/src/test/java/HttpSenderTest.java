import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import uk.gov.dvla.oepp.audit.service.Configuration;
import uk.gov.dvla.oepp.audit.service.consumer.HttpSender;

import static javax.ws.rs.client.ClientBuilder.newClient;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class HttpSenderTest {

    private MockServerClient server;
    private HttpSender sender;

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);

    @Before
    public void init() {
        server = new MockServerClient("localhost", mockServerRule.getPort());
        sender = new HttpSender(newClient(), configuration("http://localhost:" + mockServerRule.getPort() + "/audit-endpoint"));
    }

    private Configuration.AuditEndpoint configuration(String url) {
        Configuration.AuditEndpoint configuration = new Configuration.AuditEndpoint();
        configuration.setUrl(url);
        return configuration;
    }

    @Test
    public void shouldReturnTrueWhenMessageWasSent() {
        server.when(request()).respond(response().withStatusCode(200).withBody(":)"));

        boolean delivered = sender.sendAudit("Hello world");
        assertThat(delivered, is(true));

        server.verify(request()
                .withPath("/audit-endpoint")
                .withHeader("Content-Type: application/json")
                .withBody("Hello world")
        );
    }

    @Test
    public void shouldReturnFalseWhenMessageWasNotSent() {
        server.when(request()).respond(response().withStatusCode(502).withBody(":(")); // beloved proxy error

        boolean delivered = sender.sendAudit("Hello world");
        assertThat(delivered, is(false));

        server.verify(request()
                .withPath("/audit-endpoint")
                .withHeader("Content-Type: application/json")
                .withBody("Hello world")
        );
    }

    @Test
    public void shouldReturnFalseWhenClientFailedToConnectToEndpoint() {
        server.stop();

        boolean delivered = sender.sendAudit("Hello world");
        assertThat(delivered, is(false));
    }

}
