#!/usr/bin/env bash
set -e
export TERM=ansi
export AWS_ACCESS_KEY_ID=foobar
export AWS_SECRET_ACCESS_KEY=foobar
export AWS_DEFAULT_REGION=eu-west-2
export PAGER=

aws --endpoint-url=http://localhost:4575 sns create-topic --name offender_events

aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name Digital-Prison-Services-dev-hmpps_tier_offender_events_queue
aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name Digital-Prison-Services-dev-hmpps_tier_offender_events_queue_d

aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name Digital-Prison-Services-dev-hmpps_tier_offender_events_queue
aws --endpoint-url=http://localhost:4576 sqs set-queue-attributes --queue-url "http://localhost:4576/queue/Digital-Prison-Services-dev-hmpps_tier_offender_events_queue" --attributes '{"RedrivePolicy":"{\"maxReceiveCount\":\"3\", \"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:Digital-Prison-Services-dev-hmpps_tier_offender_events_queue_d\"}"}'
aws --endpoint-url=http://localhost:4575 sns subscribe \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events \
    --protocol sqs \
    --notification-endpoint http://localhost:4576/queue/Digital-Prison-Services-dev-hmpps_tier_offender_events_queue \
    --attributes '{"FilterPolicy":"{\"eventType\":[\"OFFENDER_MANAGEMENT_TIER_CALCULATION_REQUIRED\"]}"}'


aws --endpoint-url=http://localhost:4575 sns publish \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events \
    --message-attributes '{"eventType": {"DataType": "String","StringValue": "OFFENDER_MANAGEMENT_TIER_CALCULATION_REQUIRED"},"source": {"DataType": "String","StringValue": "delius"},"id": {"DataType": "String","StringValue": "fcf89ef7-f6e8-ee95-326f-8ce87d3b8ea0"},"contentType": {"DataType": "String","StringValue": "text/plain;charset=UTF-8"},"timestamp": {"DataType": "Number","StringValue": "1611149702333"}}' \
    --message '{"offenderId":2500468261,"crn":"X373878","sourceId":11174,"eventDatetime":"2021-01-20T13:34:59"}'

echo All Ready
