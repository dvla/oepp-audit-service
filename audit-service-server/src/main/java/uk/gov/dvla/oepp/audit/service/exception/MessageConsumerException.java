package uk.gov.dvla.oepp.audit.service.exception;

/**
 * Exception used when the {@link uk.gov.dvla.oepp.audit.service.consumer.AbstractConsumer} fails.
 */
public class MessageConsumerException extends RuntimeException {

    public MessageConsumerException(String message, Exception e) {
        super(message, e);
    }

}
