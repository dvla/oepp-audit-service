package uk.gov.dvla.oepp.audit.service.client;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Audit Producer to be used by the clients such as customer-portal.
 * <p>
 * Connection will be setup on initialisation.
 */
public class AuditProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditProducer.class);
    private static final String ROUTING_KEY = "audit";

    private Channel channel;
    private Connection connection;

    private String host;
    private int port;
    private String username;
    private String password;
    private String exchangeName;

    public AuditProducer(String host, int port, String username, String password, String exchangeName) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.exchangeName = exchangeName;
    }

    /**
     * Initialise the connection for the amqp provider.
     */
    public void initialise() throws IOException, TimeoutException {
        LOGGER.debug("Creating connection to host: " + host);
        initialiseConnection();

        channel = connection.createChannel();
        channel.exchangeDeclarePassive(exchangeName);
        channel.confirmSelect();
    }

    private void initialiseConnection() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setAutomaticRecoveryEnabled(true);
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);

        connection = factory.newConnection();
    }

    /**
     * Send audit message to exchange. It will be routed to the correct queue using the routing key provided to the constructor.
     * The message sent will be persistent (MessageProperties.PERSISTENT_BASIC)
     *
     * @param message audit message
     * @throws IOException
     */
    public void sendAudit(String message) throws IOException {
        channel.basicPublish(exchangeName, ROUTING_KEY, true, MessageProperties.PERSISTENT_BASIC, message.getBytes());
    }

    /**
     * Close connections.
     *
     * @throws IOException
     * @throws TimeoutException
     */
    public void close() throws IOException, TimeoutException {
        LOGGER.debug("Closing the connection to rabbitmq");
        channel.close();
        connection.close();
    }
}
