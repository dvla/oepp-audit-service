import org.junit.Before;
import org.junit.Test;
import uk.gov.dvla.oepp.audit.service.Application;
import uk.gov.dvla.oepp.audit.service.consumer.AuditConsumer;
import uk.gov.dvla.oepp.audit.service.consumer.ErrorConsumer;
import uk.gov.dvla.oepp.audit.service.exception.ConfigurationException;
import uk.gov.dvla.oepp.audit.service.exception.InvalidArgumentException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ApplicationTest {

    private Application application;

    @Before
    public void init() {
        application = new Application();
    }

    @Test
    public void applicationThrowsExceptionWhenGivenZeroArguments() {
        try {
            application.run(new String[] {});
            fail("Expected exception not thrown");
        } catch (InvalidArgumentException ex) {
            assertThat(ex.getMessage(), is("Incorrect number of arguments - expected configuration file path"));
        }
    }

    @Test
    public void applicationThrowsExceptionWhenGivenMoreThenOneArgument() {
        try {
            application.run(new String[] {"a", "b"});
            fail("Expected exception not thrown");
        } catch (InvalidArgumentException ex) {
            assertThat(ex.getMessage(), is("Incorrect number of arguments - expected configuration file path"));
        }
    }

    @Test
    public void applicationThrowsExceptionGivenIncorrectFilePath() {
        try {
            application.run(new String[] {"non-existing.yaml"});
            fail("Expected exception not thrown");
        } catch (InvalidArgumentException ex) {
            assertThat(ex.getMessage(), is("Configuration file non-existing.yaml does not exist"));
        }
    }

    @Test
    public void applicationThrowsExceptionGivenInvalidFileFormat() throws InvalidArgumentException {
        try {
            application.run(new String[] {"src/test/resources/invalid-config.txt"});
            fail("Expected exception not thrown");
        } catch (ConfigurationException ex) {
            assertThat(ex.getMessage(), is("Unable to load src/test/resources/invalid-config.txt configuration file"));
        }
    }

    @Test
    public void applicationThrowsExceptionGivenInvalidFileContent() throws InvalidArgumentException {
        try {
            application.run(new String[] {"src/test/resources/invalid-config.yaml"});
            fail("Expected exception not thrown");
        } catch (ConfigurationException ex) {
            assertThat(ex.getMessage(), is("Configuration file src/test/resources/invalid-config.yaml is invalid because: message broker configuration is required"));
        }
    }

    @Test
    public void applicationRunsGivenCorrectFilePath() {
        AuditConsumer auditConsumer = mock(AuditConsumer.class);
        application.setAuditConsumer(auditConsumer);
        ErrorConsumer errorConsumer = mock(ErrorConsumer.class);
        application.setErrorConsumer(errorConsumer);
        try {
            application.run(new String[] {"src/test/resources/valid-config.yaml"});
        } catch (InvalidArgumentException e) {
            fail("Unexpected exception thrown");
        }

        verify(auditConsumer).startConsumer();
    }
}
