package uk.gov.dvla.oepp.audit.service.consumer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dvla.oepp.audit.service.Configuration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

/**
 * HTTP sender to post messages to audit endpoint.
 */
public class HttpSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpSender.class);

    private final WebTarget target;

    public HttpSender(Client client, Configuration.AuditEndpoint configuration) {
        target = client.target(configuration.getUrl());
    }

    /**
     * Attempts to send audit message to the audit endpoint
     *
     * @param message audit message
     * @return whether or not the message was delivered successfully
     */
    public boolean sendAudit(String message) {
        try {
            Response response = target.request().post(entity(message, APPLICATION_XML));
            boolean delivered = response.getStatusInfo().getFamily() == Family.SUCCESSFUL;

            if (delivered) {
                LOGGER.debug("Received successful response {} for message {}", response.readEntity(String.class), message);
            } else {
                LOGGER.error("Received {} response for message {}, response body was {}", response.getStatus(), message, response.readEntity(String.class));
            }

            return delivered;
        } catch (Exception ex) {
            LOGGER.error("Unexpected error ({}) occurred while calling underlying service for message: {}", ex.getMessage(), message);
            return false;
        }
    }

}
