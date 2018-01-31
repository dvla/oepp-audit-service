audit-service-server
========

This project consumes messages from a message broker (such as rabbitmq) and sends them to an audit endpoint.

## What does it do with the messages?

The application tries to send the messages from the audit queue to the audit endpoint.
If this fails it moves them to the retry queue where they will sit until the `x-message-ttl` on the retry queue has elapsed.
Once this has elapsed they will be moved back to the audit queue. This is repeated until it `maxRetries` is reached. At that point they will be moved to the error queue.

## How does the retry system work?

The audit and retry queues make use of [dead lettering](https://www.rabbitmq.com/dlx.html).
Dead Lettering is a way to handle messages that are undeliverable. We provide the queue with an exchange name, and routing key, it then uses these for dead lettering.
When a message fails to send or is rejected we then Nack the message turning it into a dead letter, this adds headers to the message such as why it was dead lettered and the number of times it was dead lettered
Once it is dead lettered it is routed to the dead letter exchange using the dead letter routing key that we defined when creating the queue.

The retry queue has a [TTL](https://www.rabbitmq.com/ttl.html), messages live on this queue for a set period of time before being dead lettered.
The dead letter queue for the retry queue is the audit queue, so the messages are returned there.
We can check the headers and route to the error queue after a certain number of retries. 
Messages that are routed to the error queue are published with the same headers as the last time that they failed to send so that these headers can be analysed later if necessary. For example, messages on the error queue will still have their dead letter headers.

## How to resend messages from the error queue?

In order to resend the messages from the error queue simply change the `requeueErrors` value in the configuration file to true.
Change it back again to stop messages in the error queue from being requeued.
You can do this while the application is running.

The error consumer will strip away all existing headers (such as dead letter headers) excluding the message ID, in order to create a 'fresh' message for the audit consumer, so that the retry system can handle it correctly.

## How to send messages over HTTPS?

The application uses `Jersey` client which is HTTPS ready and no extra configuration is required when valid certificates are used.

In case certificates are not publicly trusted (e.g. self-signed certificates), application needs to trust these certificates and following configuration is needed:

```
httpClient:
  tls:
    trustStorePath: <path to client truststore in JKS format>
    trustStorePassword: <client truststore password>
```

In case client authentication is required on the server side as well (mutual TLS) then following configuration is also needed:

```
httpClient:
  tls:
    keyStorePath: <path to client keystore in JKS format>
    keyStorePassword: <client keystore password>
```

## How to adjust HTTP client timeouts?

Default timeouts are `500` milliseconds which might be too aggressive in some environments. To increase these timeouts following configuration is needed:

```
httpClient:
  connectTimeoutInMilliseconds: <timeout value in milliseconds>
  readTimeoutInMilliseconds: <timeout value in milliseconds>
```

- `connectTimeoutInMilliseconds` - timeout in milliseconds, to be used when opening a communication link to the resource. If the timeout expires before the connection can be established, a `java.net.SocketTimeoutException` is raised. A timeout of zero is interpreted as an infinite timeout.
- `readTimeoutInMilliseconds` - timeout in milliseconds, to be used when reading from input stream when a connection is established to a resource. If the timeout expires before there is data available for read, a java.net.SocketTimeoutException is raised. A timeout of zero is interpreted as an infinite timeout.