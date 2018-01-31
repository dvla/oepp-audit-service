package uk.gov.dvla.oepp.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dvla.oepp.audit.service.consumer.AuditConsumer;
import uk.gov.dvla.oepp.audit.service.consumer.ErrorConsumer;
import uk.gov.dvla.oepp.audit.service.consumer.HttpSender;
import uk.gov.dvla.oepp.audit.service.exception.ConfigurationException;
import uk.gov.dvla.oepp.audit.service.exception.InvalidArgumentException;
import uk.gov.dvla.oepp.audit.service.exception.MessageConsumerException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.stream.Collectors;

import static uk.gov.dvla.oepp.audit.service.consumer.http.JerseyClientFactory.createClient;

/**
 * Begins the audit consumer based on a configuration file which must be provided as a command line arguments
 * The configuration file will be validated. The program will stop if any of the configuration is missing or if the file does not exist
 */
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static final int TIME_MILLIS_BETWEEN_CONFIG_CHECKS = 5000;

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private AuditConsumer auditConsumer;
    private ErrorConsumer errorConsumer;

    /**
     * Runner expects a single argument provided with the file path to YAML configuration file.
     *
     * @param args - application arguments
     */
    public static void main(String[] args) {
        try {
            new Application().run(args);
            LOGGER.debug("Audit consumer started");
        } catch (InvalidArgumentException | ConfigurationException | MessageConsumerException ex) {
            LOGGER.error(ex.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs application using provided arguments
     *
     * @param args - application arguments
     * @throws InvalidArgumentException when application was launched using invalid arguments
     * @throws ConfigurationException   when configuration file reading or validation issue occurred
     * @throws MessageConsumerException when consumer initialisation failed or cannot start receiving messages
     */
    public void run(String[] args) throws InvalidArgumentException {
        validateArguments(args);

        runConsumers(loadConfiguration(args[0]));

        new Timer().schedule(new ConfigurationChangeWatcher(new File(args[0])), new Date(), TIME_MILLIS_BETWEEN_CONFIG_CHECKS);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                LOGGER.info("Closing message broker connections on shutdown");
                auditConsumer.close();
                errorConsumer.close();
            }
        });
    }

    private void validateArguments(String[] args) throws InvalidArgumentException {
        if (args.length != 1) {
            throw new InvalidArgumentException("Incorrect number of arguments - expected configuration file path");
        }

        if (!new File(args[0]).exists()) {
            throw new InvalidArgumentException("Configuration file " + args[0] + " does not exist");
        }
    }

    private Configuration loadConfiguration(String filePath) {
        Configuration configuration;

        try {
            configuration = objectMapper.readValue(new File(filePath), Configuration.class);
        } catch (IOException ex) {
            throw new ConfigurationException("Unable to load " + filePath + " configuration file", ex);
        }

        String violationMessages = validator.validate(configuration).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        if (!violationMessages.isEmpty()) {
            throw new ConfigurationException("Configuration file " + filePath + " is invalid because: " + violationMessages);
        }

        return configuration;
    }

    private void runConsumers(Configuration configuration) {
        HttpSender sender = new HttpSender(createClient(configuration.getHttpClient()), configuration.getAuditEndpoint());

        if (auditConsumer == null) {
            auditConsumer = new AuditConsumer(configuration.getMessageBroker(), sender);
        }

        auditConsumer.initialiseConnection();
        auditConsumer.startConsumer();

        if (errorConsumer == null) {
            errorConsumer = new ErrorConsumer(configuration.getMessageBroker());
        }

        errorConsumer.initialiseConnection();

        if (configuration.getMessageBroker().getRequeueErrors()) {
            errorConsumer.startConsumer();
        }
    }

    public void setAuditConsumer(AuditConsumer auditConsumer) {
        this.auditConsumer = auditConsumer;
    }

    public void setErrorConsumer(ErrorConsumer errorConsumer) {
        this.errorConsumer = errorConsumer;
    }

    private class ConfigurationChangeWatcher extends AbstractFileChangeWatcher {

        private Configuration lastKnownConfiguration;

        public ConfigurationChangeWatcher(File file) {
            super(file);
            lastKnownConfiguration = loadConfiguration(file.getPath());
        }

        @Override
        protected void onChange(File file) {
            Configuration changedConfiguration = loadConfiguration(file.getPath());
            if (lastKnownConfiguration.getMessageBroker().getRequeueErrors() != changedConfiguration.getMessageBroker().getRequeueErrors()) {
                if (changedConfiguration.getMessageBroker().getRequeueErrors()) {
                    errorConsumer.startConsumer();
                } else {
                    errorConsumer.stopConsumer();
                }

                lastKnownConfiguration = changedConfiguration;
            }
        }
    }
}
