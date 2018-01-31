package uk.gov.dvla.oepp.audit.service.consumer;

import com.rabbitmq.client.*;
import uk.gov.dvla.oepp.audit.service.Configuration.MessageBroker;
import uk.gov.dvla.oepp.audit.service.exception.MessageConsumerException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A consumer of audit messages that will retrieve messages from the audit queue and forward them to the audit endpoint.
 */
public class AuditConsumer extends AbstractConsumer {

    private final HttpSender httpSender;
    private final String exchangeName;
    private final String queueName;
    private final int maxRetries;

    public AuditConsumer(MessageBroker configuration, HttpSender httpSender) {
        super(configuration.getHost(), configuration.getPort(), configuration.getUsername(), configuration.getPassword());
        this.httpSender = httpSender;
        this.exchangeName = configuration.getExchangeName();
        this.queueName = configuration.getQueueName();
        this.maxRetries = configuration.getMaxRetries();
    }

    /**
     * Begins taking messages from the audit queue and tries to send them to the audit endpoint
     *
     * @throws IllegalStateException when {@link AuditConsumer#initialiseConnection()} has not been called before this method
     * @throws MessageConsumerException when cannot begin message consumption due to message broker IO errors
     */
    public void startConsumer() {
        if (connection.isPresent()) {
            Connection connection = this.connection.get();
            try {
                Channel channel = connection.createChannel();
                channel.confirmSelect();

                channel.basicConsume(queueName, false, new DeliveryHandler(channel, httpSender, maxRetries, exchangeName));
                LOGGER.debug("Consumer created and listening for messages from queue: " + messageBrokerAddress() + ":" + port);
                this.channel = Optional.of(channel);
            } catch (IOException ex) {
                throw new MessageConsumerException("Unable to consume messages from " + messageBrokerAddress(), ex);
            }
        } else {
            throw new IllegalStateException("Connection has not been initialised");
        }
    }

    static class DeliveryHandler extends DefaultConsumer {

        private static final String ROUTING_KEY_ERROR = "error";

        private final HttpSender sender;
        private final int maxRetries;
        private final String exchangeName;

        /**
         * Constructs a new instance and records its association to the passed-in channel.
         *
         * @param channel channel to which this consumer is attached
         * @param sender sender which sends messages to audit endpoint
         * @param maxRetries number of times message is tried to send before failing over to error queue
         * @param exchangeName exchange used together with {@link DeliveryHandler#ROUTING_KEY_ERROR} to publish message to error queue
         */
        DeliveryHandler(Channel channel, HttpSender sender, int maxRetries, String exchangeName) {
            super(channel);
            this.maxRetries = maxRetries;
            this.exchangeName = exchangeName;
            this.sender = sender;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
            String message = new String(body);
            LOGGER.debug("Received audit message {}", message);

            boolean messageMovedToErrorQueue = false;
            if (retriesCount(properties.getHeaders()) > maxRetries) {
                LOGGER.error("Message repeatedly failed to send, publishing message to error queue: {}", message);
                getChannel().basicPublish(exchangeName, ROUTING_KEY_ERROR, true, MessageProperties.PERSISTENT_BASIC, message.getBytes());
                messageMovedToErrorQueue = true;
            }

            if (messageMovedToErrorQueue || sender.sendAudit(message)) {
                getChannel().basicAck(envelope.getDeliveryTag(), false);
            } else {
                LOGGER.debug("Message failed to send, rerouting to retry queue: {}", message);
                getChannel().basicNack(envelope.getDeliveryTag(), false, false);
            }
        }

        private long retriesCount(Map<String, ?> messageHeaders) {
            if (messageHeaders != null) {
                List<Map<String, Object>> deaths = (List<Map<String, Object>>) messageHeaders.get("x-death");

                if (deaths != null) {
                    return deaths.stream().filter(this::deathDueToRejection).count();
                }
            }
            return 0;
        }

        private boolean deathDueToRejection(Map<String, Object> death) {
            Object deathReason = death.get("reason");
            return deathReason != null && deathReason.toString().equals("rejected");
        }
    }
}