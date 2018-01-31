package uk.gov.dvla.oepp.audit.service.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import uk.gov.dvla.oepp.audit.service.exception.MessageConsumerException;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public abstract class AbstractConsumer {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractConsumer.class);

    protected final String host;
    protected final Integer port;
    protected final String username;
    protected final String password;

    protected Optional<Connection> connection  = Optional.empty();
    protected Optional<Channel> channel = Optional.empty();

    protected AbstractConsumer(String host, Integer port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * Sets up the connection to the message broker and attempts to connect
     *
     * @throws MessageConsumerException - when IO error or connection timeout occurred
     */
    public void initialiseConnection() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setAutomaticRecoveryEnabled(true);

        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        try {
            connection = Optional.of(factory.newConnection());
        } catch (IOException ex) {
            throw new MessageConsumerException("Unable to initiate connection to " + messageBrokerAddress() + " - IO exception occurred", ex);
        } catch (TimeoutException ex) {
            throw new MessageConsumerException("Unable to initiate connection to " + messageBrokerAddress() + " - connecting timeout occurred", ex);
        }
    }

    protected String messageBrokerAddress() {
        return host + ":" + port;
    }

    public abstract void startConsumer();

    /**
     * Closes the channel and connection to the message broker if they exist
     */
    public void close() {
        channel.ifPresent(channel -> {
            try {
                channel.close();
            } catch (Exception ex) {
                LOGGER.warn("Failed to close channel");
            }
        });
        connection.ifPresent(connection -> {
            try {
                connection.close();
            } catch (Exception ex) {
                LOGGER.warn("Failed to close connection");
            }
        });
    }
}
