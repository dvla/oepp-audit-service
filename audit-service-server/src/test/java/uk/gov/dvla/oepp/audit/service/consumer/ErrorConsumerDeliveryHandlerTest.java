package uk.gov.dvla.oepp.audit.service.consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErrorConsumerDeliveryHandlerTest {

    private static final String EXCHANGE_NAME = "ExchangeName";
    private static final String ROUTING_KEY = "audit";

    private ErrorConsumer.DeliveryHandler deliveryHandler;

    private Channel channel;
    private Envelope envelope;

    @Before
    public void init() {
        channel = mock(Channel.class);

        deliveryHandler = new ErrorConsumer.DeliveryHandler(channel, EXCHANGE_NAME);

        envelope = mock(Envelope.class);
        when(envelope.getDeliveryTag()).thenReturn(1L);
    }

    @Test
    public void messageShouldBePublishedToAuditQueueAndAcknowledged() throws IOException {
        String message = "message";

        deliveryHandler.handleDelivery("Tag", envelope, MessageProperties.PERSISTENT_BASIC, message.getBytes());

        verify(channel).basicPublish(EXCHANGE_NAME, ROUTING_KEY, true, MessageProperties.PERSISTENT_BASIC, message.getBytes());
        verify(channel).basicAck(1L, false);
    }
}
