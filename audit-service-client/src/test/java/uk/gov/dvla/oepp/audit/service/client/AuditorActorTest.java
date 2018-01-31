package uk.gov.dvla.oepp.audit.service.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.dvla.oepp.audit.service.message.AuditMessage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuditorActorTest {

    private static ActorSystem system;

    @Mock
    private AuditProducer producer;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testActorSendSuccess() throws IOException, TimeoutException {
        SampleAuditMessage auditMessage = new SampleAuditMessage();
        String expectedXML = "<sample-audit-message timestamp=\"" + new SimpleDateFormat("dd-MM-YYYY HH:mm:ss").format(auditMessage.timestamp) + "\"/>";

        new JavaTestKit(system) {
            {
                final ActorRef auditor = system.actorOf(Props.create(AuditorActor.class, producer));

                auditor.tell(auditMessage, getRef());
                expectNoMsg();

                verify(producer, times(1)).sendAudit(expectedXML);
                verifyNoMoreInteractions(producer);
            }
        };
    }

    @Test
    public void testActorSendInvalidMessage() throws IOException, TimeoutException {
        new JavaTestKit(system) {
            {
                final ActorRef auditor = system.actorOf(Props.create(AuditorActor.class, producer));

                auditor.tell("Invalid Message", getRef());
                expectNoMsg();

                verifyZeroInteractions(producer);
            }
        };
    }

    @JacksonXmlRootElement(localName = "sample-audit-message")
    public static class SampleAuditMessage implements AuditMessage {

        @JacksonXmlProperty(isAttribute = true)
        private final Date timestamp = new Date();

    }
}
