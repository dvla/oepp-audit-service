import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.After;
import uk.gov.dvla.oepp.audit.service.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

class BaseConsumerTestIT {

    private static final String HOST = "localhost";
    private static final int PORT = 5672;
    private static final String USERNAME = "xxxx";
    private static final String PASSWORD = "xxxx";

    private static final String EXCHANGE_TYPE = "direct";
    static final String EXCHANGE_NAME = "OEPPAuditTestExchange";

    static final String QUEUE = "oepp.audit.test.queue";
    static final String RETRY_QUEUE = "oepp.audit.test.queue.retry";
    static final String ERROR_QUEUE = "oepp.audit.test.queue.error";

    static final String ROUTING_KEY_AUDIT = "audit";
    static final String ROUTING_KEY_RETRY = "retry";
    static final String ROUTING_KEY_ERROR = "error";

    private Connection connection;
    Channel channel;

    @After
    public void tearDown() throws IOException, TimeoutException, InterruptedException {
        channel.close();
        connection.close();
    }

    Configuration.MessageBroker messageBrokerConfiguration() {
        Configuration.MessageBroker messageBroker = new Configuration.MessageBroker();
        messageBroker.setHost(HOST);
        messageBroker.setPort(PORT);
        messageBroker.setUsername(USERNAME);
        messageBroker.setPassword(PASSWORD);
        messageBroker.setExchangeName(EXCHANGE_NAME);
        messageBroker.setQueueName(QUEUE);
        messageBroker.setErrorQueueName(ERROR_QUEUE);
        messageBroker.setRetryQueueName(RETRY_QUEUE);
        messageBroker.setMaxRetries(0);
        messageBroker.setRequeueErrors(false);
        return messageBroker;
    }

    void initialiseMessageBroker() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setAutomaticRecoveryEnabled(true);
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);

        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, EXCHANGE_TYPE, true);

        declareQueues();
    }

    private void declareQueues() throws IOException {
        // QUEUE
        deleteQueueIfExists(QUEUE);
        channel.queueDeclare(QUEUE, true, false, false, ImmutableMap.<String, Object>builder()
                .put("x-dead-letter-exchange", EXCHANGE_NAME)
                .put("x-dead-letter-routing-key", ROUTING_KEY_RETRY)
                .build());
        channel.queueBind(QUEUE, EXCHANGE_NAME, ROUTING_KEY_AUDIT);

        // RETRY QUEUE
        deleteQueueIfExists(RETRY_QUEUE);
        channel.queueDeclare(RETRY_QUEUE, true, false, false, ImmutableMap.<String, Object>builder()
                .put("x-dead-letter-exchange", EXCHANGE_NAME)
                .put("x-dead-letter-routing-key", ROUTING_KEY_AUDIT)
                .put("x-message-ttl", 5000)
                .build());
        channel.queueBind(RETRY_QUEUE, EXCHANGE_NAME, ROUTING_KEY_RETRY);

        // ERROR QUEUE
        deleteQueueIfExists(ERROR_QUEUE);
        channel.queueDeclare(ERROR_QUEUE, true, false, false, null);
        channel.queueBind(ERROR_QUEUE, EXCHANGE_NAME, ROUTING_KEY_ERROR);
    }

    private void deleteQueueIfExists(String queueName) throws IOException {
        try {
            channel.queueDelete(queueName);
        } catch (Exception ex) {
            channel = connection.createChannel();
        }
    }
}
