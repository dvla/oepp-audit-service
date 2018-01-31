package uk.gov.dvla.oepp.audit.service.client;

import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dvla.oepp.audit.service.message.AuditMessage;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Audit Actor is responsible for constructing the underlying message type and
 * publishing the message for the audit to the amqp exchange. The exchange itself will be
 * setup and utilised via the audit-service-client and is not natively part of the
 * customer portal.
 */
public class AuditorActor extends UntypedActor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditorActor.class);

    private final AuditProducer producer;
    private final ObjectMapper mapper;

    public AuditorActor(AuditProducer producer) {
        this.producer = producer;
        this.mapper = defaultMapper();
    }

    private ObjectMapper defaultMapper() {
        return new XmlMapper().setDateFormat(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"));
    }

    public void onReceive(Object message) throws IOException {
        if (message instanceof AuditMessage) {
            producer.sendAudit(mapper.writeValueAsString(message));
        } else {
            LOGGER.warn("Unsupported message type " + message.getClass().getName());
            unhandled(message);
        }
    }

}