import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import uk.gov.dvla.oepp.audit.service.Configuration;
import uk.gov.dvla.oepp.audit.service.consumer.AuditConsumer;
import uk.gov.dvla.oepp.audit.service.consumer.HttpSender;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.sleep;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;
import static uk.gov.dvla.oepp.audit.service.consumer.http.JerseyClientFactory.createClient;

public class AuditConsumerTestIT extends BaseConsumerTestIT {

    private AuditConsumer auditConsumer;

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);
    private MockServerClient server;

    @Before
    public void init() throws IOException, TimeoutException {
        initialiseMessageBroker();

        server = new MockServerClient("localhost", mockServerRule.getPort());

        auditConsumer = new AuditConsumer(messageBrokerConfiguration(), new HttpSender(client(), auditEndpointConfiguration()));
        auditConsumer.initialiseConnection();
        auditConsumer.startConsumer();
    }

    private Client client() {
        return createClient(new Configuration.HttpClient());
    }

    private Configuration.AuditEndpoint auditEndpointConfiguration() {
        Configuration.AuditEndpoint auditEndpoint = new Configuration.AuditEndpoint();
        auditEndpoint.setUrl("http://localhost:" + mockServerRule.getPort() + "/dsd/audit");
        return auditEndpoint;
    }

    @After
    public void stopConsumer() {
        auditConsumer.close();
    }

    @Test
    public void messageShouldBeSentToAuditEndpoint() throws IOException, TimeoutException, InterruptedException {
        String message = "audit-message";
        server.when(request()).respond(response().withStatusCode(200));

        channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY_AUDIT, true, MessageProperties.PERSISTENT_BASIC, message.getBytes());
        sleep(3000);

        server.verify(request().withMethod("POST").withPath("/dsd/audit").withBody(message), exactly(1));
    }

    @Test
    public void messageThatFailedToSendOnceShouldGoToRetryQueue() throws IOException, InterruptedException {
        String message = "failed-audit-message";
        server.when(request()).respond(response().withStatusCode(400));

        channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY_AUDIT, true, MessageProperties.PERSISTENT_BASIC, message.getBytes());
        sleep(3000);

        server.verify(request().withMethod("POST").withPath("/dsd/audit").withBody(message), exactly(1));
        GetResponse response = channel.basicGet(RETRY_QUEUE, true);
        assertThat(new String(response.getBody()), is(message));
    }

    @Test
    public void messageThatFailsRepeatedlyShouldGoToErrorQueue() throws IOException, InterruptedException {
        String message = "error-audit-message";
        server.when(request()).respond(response().withStatusCode(400));

        channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY_AUDIT, true, MessageProperties.PERSISTENT_BASIC, message.getBytes());
        sleep(6000);

        server.verify(request().withMethod("POST").withPath("/dsd/audit").withBody(message), exactly(1));
        GetResponse response = channel.basicGet(ERROR_QUEUE, true);
        assertThat(new String(response.getBody()), is(message));
    }
}
