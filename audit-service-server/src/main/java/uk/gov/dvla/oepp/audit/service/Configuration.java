package uk.gov.dvla.oepp.audit.service;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class Configuration {

    @NotNull(message = "message broker configuration is required")
    @Valid
    private MessageBroker messageBroker;
    @NotNull(message = "audit endpoint configuration is required")
    @Valid
    private AuditEndpoint auditEndpoint;
    @Valid
    private HttpClient httpClient = new HttpClient();

    public MessageBroker getMessageBroker() {
        return messageBroker;
    }

    public AuditEndpoint getAuditEndpoint() {
        return auditEndpoint;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public static class MessageBroker {

        @NotNull(message = "message broker host is required")
        private String host = "localhost";
        @NotNull(message = "message broker port is required")
        private Integer port = 5672;
        @NotEmpty(message = "message broker username is required")
        private String username;
        @NotEmpty(message = "message broker password is required")
        private String password;
        @NotEmpty(message = "message broker queue name is required")
        private String queueName;
        @NotEmpty(message = "message broker retry queue name is required")
        private String retryQueueName;
        @NotEmpty(message = "message broker error queue name is required")
        private String errorQueueName;
        @NotEmpty(message = "message broker exchange name is required")
        private String exchangeName;
        @NotNull(message = "message broker requeue errors value is required")
        private Boolean requeueErrors;
        @NotNull(message = "message broker max retries is required")
        private Integer maxRetries;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getQueueName() {
            return queueName;
        }

        public void setQueueName(String queueName) {
            this.queueName = queueName;
        }

        public String getRetryQueueName() {
            return retryQueueName;
        }

        public void setRetryQueueName(String retryQueueName) {
            this.retryQueueName = retryQueueName;
        }

        public String getErrorQueueName() {
            return errorQueueName;
        }

        public void setErrorQueueName(String errorQueueName) {
            this.errorQueueName = errorQueueName;
        }

        public String getExchangeName() {
            return exchangeName;
        }

        public void setExchangeName(String exchangeName) {
            this.exchangeName = exchangeName;
        }

        public Boolean getRequeueErrors() {
            return requeueErrors;
        }

        public void setRequeueErrors(Boolean requeueErrors) {
            this.requeueErrors = requeueErrors;
        }

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }
    }

    public static class AuditEndpoint {

        @NotEmpty(message = "audit endpoint URL is required")
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class HttpClient {

        @Min(value = 0, message = "connect timeout must be greater or equal zero")
        private int connectTimeoutInMilliseconds = 500;
        @Min(value = 0, message = "read timeout must be greater or equal zero")
        private int readTimeoutInMilliseconds = 500;
        @Valid
        private TLS tls;

        public int getConnectTimeoutInMilliseconds() {
            return connectTimeoutInMilliseconds;
        }

        public int getReadTimeoutInMilliseconds() {
            return readTimeoutInMilliseconds;
        }

        public TLS getTLS() {
            return tls;
        }

        public static class TLS {

            private String keyStorePath;
            private String keyStorePassword;
            private String trustStorePath;
            private String trustStorePassword;

            public String getKeyStorePath() {
                return keyStorePath;
            }

            public String getKeyStorePassword() {
                return keyStorePassword;
            }

            public String getTrustStorePath() {
                return trustStorePath;
            }

            public String getTrustStorePassword() {
                return trustStorePassword;
            }
        }

    }
}
