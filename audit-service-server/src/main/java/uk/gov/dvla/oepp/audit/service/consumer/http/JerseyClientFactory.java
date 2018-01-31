package uk.gov.dvla.oepp.audit.service.consumer.http;

import org.glassfish.jersey.SslConfigurator;
import uk.gov.dvla.oepp.audit.service.Configuration;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;

public class JerseyClientFactory {

    public static Client createClient(Configuration.HttpClient configuration) {
        return ClientBuilder.newBuilder()
                .sslContext(sslContext(configuration.getTLS()))
                .property(CONNECT_TIMEOUT, configuration.getConnectTimeoutInMilliseconds())
                .property(READ_TIMEOUT, configuration.getReadTimeoutInMilliseconds())
                .build();
    }

    private static SSLContext sslContext(Configuration.HttpClient.TLS configuration) {
        SslConfigurator configurator = SslConfigurator.newInstance(true);

        if (configuration != null && configuration.getKeyStorePath() != null) {
            configurator.keyStoreFile(configuration.getKeyStorePath());
            configurator.keyStorePassword(configuration.getKeyStorePassword());
        }

        if (configuration != null && configuration.getTrustStorePath() != null) {
            configurator.trustStoreFile(configuration.getTrustStorePath());
            configurator.trustStorePassword(configuration.getTrustStorePassword());
        }

        return configurator.createSSLContext();
    }

}
