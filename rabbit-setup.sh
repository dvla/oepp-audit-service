#!/bin/bash
echo "Attempting to delete queues"
echo "Audit queue:"
rabbitmqadmin delete queue name=oepp.audit.queue
echo "Retry queue:"
rabbitmqadmin delete queue name=oepp.audit.queue.retry
echo "Error queue:"
rabbitmqadmin delete queue name=oepp.audit.queue.error
echo "Attempting to delete exchange:"
rabbitmqadmin delete exchange name=OEPPAuditExchange
echo "Importing rabbit.config"
rabbitmqadmin import rabbit.config
