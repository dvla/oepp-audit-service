package uk.gov.dvla.oepp.audit.service.consumer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class AuditConsumerDeliveryHandlerTest {

    private static final String EXCHANGE_NAME = "ExchangeName";
    private static final int MAX_RETRIES = 1;

    private final String message = "audit-message";

    private AuditConsumer.DeliveryHandler deliveryHandler;

    private Channel channel;
    private HttpSender sender;
    private Envelope envelope;

    @Before
    public void init() {
        channel = mock(Channel.class);
        sender = mock(HttpSender.class);

        deliveryHandler = new AuditConsumer.DeliveryHandler(channel, sender, MAX_RETRIES, EXCHANGE_NAME);

        envelope = mock(Envelope.class);
        when(envelope.getDeliveryTag()).thenReturn(1L);
    }

    @Test
    public void auditMessageShouldBeAcknowledgedWhenSendSuccessful() throws IOException {
        when(sender.sendAudit(message)).thenReturn(true);

        deliveryHandler.handleDelivery("Tag", envelope, MessageProperties.PERSISTENT_BASIC, message.getBytes());

        verify(sender).sendAudit(message);
        verify(channel).basicAck(1L, false);
    }

    @Test
    public void auditMessageShouldBePublishedToRetryQueueWhenSendFails() throws IOException {
        when(sender.sendAudit(message)).thenReturn(false);

        deliveryHandler.handleDelivery("Tag", envelope, MessageProperties.PERSISTENT_BASIC, message.getBytes());

        verify(sender).sendAudit(message);
        verify(channel).basicNack(1L, false, false);
    }

    @Test
    public void auditMessageShouldBeResentWhenFirstSendFailed() throws IOException {
        AMQP.BasicProperties properties = mock(AMQP.BasicProperties.class);
        when(properties.getHeaders()).thenReturn(ImmutableMap.of("x-death", ImmutableList.of(ImmutableMap.of("reason", "rejected"))));
        when(sender.sendAudit(message)).thenReturn(true);

        deliveryHandler.handleDelivery("Tag", envelope, properties, message.getBytes());

        verify(sender).sendAudit(message);
        verify(channel).basicAck(1L, false);
    }

    @Test
    public void auditMessageShouldBePublishedToErrorQueueWhenResendFailed() throws IOException {
        AMQP.BasicProperties properties = mock(AMQP.BasicProperties.class);
        when(properties.getHeaders()).thenReturn(ImmutableMap.of("x-death", ImmutableList.of(ImmutableMap.of("reason", "rejected"), ImmutableMap.of("reason", "rejected"))));

        deliveryHandler.handleDelivery("Tag", envelope, properties, message.getBytes());

        verifyNoMoreInteractions(sender);
        verify(channel).basicPublish(EXCHANGE_NAME, "error", true, MessageProperties.PERSISTENT_BASIC, message.getBytes());
        verify(channel).basicAck(1L, false);
    }
}
