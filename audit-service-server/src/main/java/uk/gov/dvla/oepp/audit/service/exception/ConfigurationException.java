package uk.gov.dvla.oepp.audit.service.exception;

/**
 * Thrown when a configuration file IO or validation problem occurs.
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Exception e) {
        super(message, e);
    }
}
