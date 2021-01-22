#!/usr/bin/env bash
set -e
export TERM=ansi
export AWS_ACCESS_KEY_ID=foobar
export AWS_SECRET_ACCESS_KEY=foobar
export AWS_DEFAULT_REGION=eu-west-2
export PAGER=

aws --endpoint-url=http://localhost:4575 sns create-topic --name offender_events

aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name Digital-Prison-Services-dev-hmpps_tier_offender_events_queue
aws --endpoint-url=http://localhost:4576 sqs set-queue-attributes --queue-url "http://localhost:4576/queue/Digital-Prison-Services-dev-hmpps_tier_offender_events_queue" --attributes '{"RedrivePolicy":"{\"maxReceiveCount\":\"3\", \"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:prison_to_probation_dlq\"}"}'
aws --endpoint-url=http://localhost:4575 sns subscribe \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events \
    --protocol sqs \
    --notification-endpoint http://localhost:4576/queue/Digital-Prison-Services-dev-hmpps_tier_offender_events_queue \
    --attributes '{"FilterPolicy":"{\"eventType\":[ \"EXTERNAL_MOVEMENT_RECORD-INSERTED\", \"IMPRISONMENT_STATUS-CHANGED\", \"SENTENCE_DATES-CHANGED\", \"BOOKING_NUMBER-CHANGED\"] }"}'
echo All Ready
