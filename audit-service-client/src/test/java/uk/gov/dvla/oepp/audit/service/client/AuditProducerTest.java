package uk.gov.dvla.oepp.audit.service.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class AuditProducerTest {

    private static final String HOST = "host";
    private static final int PORT = 1;
    private static final String USERNAME = "xxxx";
    private static final String PASSWORD = "xxxx";
    private static final String EXCHANGE = "exchange";
    private static final String ROUTING_KEY = "audit";

    private static final String TEST_MSG = "<test />";

    @Mock
    private Channel channel;

    @InjectMocks
    private AuditProducer producer = new AuditProducer(HOST, PORT, USERNAME, PASSWORD, EXCHANGE);

    /**
     * Tests to ensure successful send of the audit - no io exception thrown
     *
     * @throws IOException
     */
    @Test
    public void testSuccessSend() throws IOException {
        doNothing().when(channel).basicPublish(EXCHANGE, ROUTING_KEY, true,
                MessageProperties.PERSISTENT_BASIC,
                TEST_MSG.getBytes());
        producer.sendAudit(TEST_MSG);

    }

    /**
     * Tests to ensure that an exception is thrown in case of channel connection issues.
     *
     * @throws IOException
     */
    @Test(expected = IOException.class)
    public void testSendIOException() throws IOException {
        doThrow(new IOException()).when(channel).basicPublish(EXCHANGE, ROUTING_KEY, true,
                MessageProperties.PERSISTENT_BASIC,
                TEST_MSG.getBytes());

        producer.sendAudit(TEST_MSG);
    }

}
