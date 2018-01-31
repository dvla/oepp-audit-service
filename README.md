audit-service
-------------

RabbitMQ (version 3.3.5 and above) is required to run this service.

In order to run audit-service-client or audit-service-server you must have the exchange and related queues that these services expect.
There is a bash script included which will perform this setup for you. In order for this script to run you must have the rabbitMQ management CLI.
It can be found here: <http://localhost:15672/cli/> (You may need to change the port to reflect the configuration of your rabbit MQ instance).

Run `rabbit-setup.sh` from within audit-service.git. This script will delete the queues and the exchange if they already exist before recreating them.
