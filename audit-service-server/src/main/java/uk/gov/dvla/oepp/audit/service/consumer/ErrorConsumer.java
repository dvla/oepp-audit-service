package uk.gov.dvla.oepp.audit.service.consumer;

import com.rabbitmq.client.*;
import uk.gov.dvla.oepp.audit.service.Configuration;
import uk.gov.dvla.oepp.audit.service.exception.MessageConsumerException;

import java.io.IOException;
import java.util.Optional;

/**
 * A consumer which takes messages from the error queue and republishes them to the audit queue
 */
public class ErrorConsumer extends AbstractConsumer implements Stoppable {

    private final String exchangeName;
    private final String errorQueueName;

    private Optional<String> consumerTag;

    public ErrorConsumer(Configuration.MessageBroker configuration) {
        super(configuration.getHost(), configuration.getPort(), configuration.getUsername(), configuration.getPassword());
        this.exchangeName = configuration.getExchangeName();
        this.errorQueueName = configuration.getErrorQueueName();
        consumerTag = Optional.empty();
    }

    /**
     * Starts moving messages on the error queue to the audit queue for reprocessing
     *
     * @throws IllegalStateException when called multiple times without calling {@link ErrorConsumer#stopConsumer()} after each
     * @throws IllegalStateException when {@link AuditConsumer#initialiseConnection()} has not been called before this method
     * @throws MessageConsumerException when cannot begin message consumption due to message broker IO errors
     */
    public void startConsumer() {
        if(consumerTag.isPresent()) {
            throw new IllegalStateException("Error Consumer is already running");
        }
        requeueErrorQueueMessages();
    }

    private void requeueErrorQueueMessages() {
        if (connection.isPresent()) {
            LOGGER.debug("Starting to requeue error messages to the audit queue");
            Connection connection = this.connection.get();
            try {
                Channel channel = connection.createChannel();
                channel.confirmSelect();

                String requeueConsumerTag = channel.basicConsume(errorQueueName, false, new DeliveryHandler(channel, exchangeName));
                this.channel = Optional.of(channel);
                consumerTag = Optional.of(requeueConsumerTag);
            } catch (IOException ex) {
                throw new MessageConsumerException("Unable to consume messages from " + messageBrokerAddress(), ex);
            }
        } else {
            throw new IllegalStateException("Connection has not been initialised");
        }
    }

    /**
     * Stops moving messages on the error queue to the audit queue for reprocessing
     *
     * @throws MessageConsumerException - when cannot stop message consumption due to message broker IO errors
     */
    public void stopConsumer() {
        channel.ifPresent(channel -> {
            consumerTag.ifPresent(requeueConsumerTag -> {
                LOGGER.debug("Stopping the error consumer");
                try {
                    channel.basicCancel(requeueConsumerTag);
                } catch (IOException ex) {
                    throw new MessageConsumerException("Unable to cancel error consumer", ex);
                }
            });
            consumerTag = Optional.empty();
        });
    }

    static class DeliveryHandler extends DefaultConsumer {

        private static final String ROUTING_KEY_AUDIT = "audit";

        private String exchangeName;

        /**
         * Constructs a new instance and records its association to the passed-in channel.
         *
         * @param channel the channel to which this consumer is attached
         * @param exchangeName exchange used together with {@link DeliveryHandler#ROUTING_KEY_AUDIT} to publish message to audit queue
         */
        public DeliveryHandler(Channel channel, String exchangeName) {
            super(channel);
            this.exchangeName = exchangeName;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
            String message = new String(body);
            try {
                getChannel().basicPublish(exchangeName, ROUTING_KEY_AUDIT, true, MessageProperties.PERSISTENT_BASIC, message.getBytes());
                getChannel().basicAck(envelope.getDeliveryTag(), false);
            } catch (IOException ex) {
                LOGGER.error("Unable to publish message from error queue to audit queue. Message was: {}", message);
            }
        }
    }
}
