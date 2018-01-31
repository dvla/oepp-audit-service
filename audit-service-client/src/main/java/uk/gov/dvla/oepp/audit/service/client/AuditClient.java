package uk.gov.dvla.oepp.audit.service.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import uk.gov.dvla.oepp.audit.service.message.AuditMessage;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static akka.pattern.Patterns.ask;

/**
 * Client to be utilised by the the customer portal to initiate the send of the
 * various audit messages. Actor is encapsulated within the client and the Actor
 * is not expected to be used directly from the portal Application Controller.
 */
public class AuditClient {

    private ActorRef auditActor;

    /**
     * Initialise the audit client.
     *
     * @param system        Actor system
     * @param host          hostname
     * @param port          port number
     * @param username      username
     * @param password      password
     * @param exchangeName  exchange name
     * @throws IOException
     * @throws TimeoutException
     */
    public void initialise(ActorSystem system, String host, int port, String username, String password, String exchangeName) throws IOException, TimeoutException {
        AuditProducer producer = new AuditProducer(host, port, username, password, exchangeName);
        producer.initialise();
        auditActor = system.actorOf(Props.create(AuditorActor.class, producer));
    }

    /**
     * Send audit message.
     *
     * @param message
     */
    public void sendAuditMessage(AuditMessage message) {
        ask(auditActor, message, 1000);
    }
}
