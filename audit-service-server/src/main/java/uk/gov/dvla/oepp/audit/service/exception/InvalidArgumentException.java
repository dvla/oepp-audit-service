package uk.gov.dvla.oepp.audit.service.exception;

import uk.gov.dvla.oepp.audit.service.Application;

/**
 * This exception is used when the {@link Application} is launched using invalid arguments
 * This covers incorrect number of parameters, invalid file and if the file does not exist
 */
public class InvalidArgumentException extends Exception {

    public InvalidArgumentException(String message) {
        super(message);
    }
}
