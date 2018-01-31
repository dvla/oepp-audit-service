import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.dvla.oepp.audit.service.consumer.ErrorConsumer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.sleep;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ErrorConsumerTestIT extends BaseConsumerTestIT {

    private ErrorConsumer errorConsumer;

    @Before
    public void init() throws IOException, TimeoutException {
        initialiseMessageBroker();

        errorConsumer = new ErrorConsumer(messageBrokerConfiguration());
        errorConsumer.initialiseConnection();
        errorConsumer.startConsumer();
    }

    @After
    public void stopConsumer() {
        errorConsumer.stopConsumer();
        errorConsumer.close();
    }

    @Test
    public void messageOnErrorQueueShouldBeMovedToAuditQueueWhenRequeueCalled() throws IOException, InterruptedException {
        String message = "moved-audit-message";

        channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY_ERROR, true, MessageProperties.PERSISTENT_BASIC, message.getBytes());

        sleep(3000);

        GetResponse response = channel.basicGet(QUEUE, true);
        assertThat(new String(response.getBody()), is(message));
    }
}
