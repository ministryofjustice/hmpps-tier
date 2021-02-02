    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: OAUTH_CLIENT_ID
    valueFrom:
      secretKeyRef:
         name: {{ template "app.name" . }}
         key: OAUTH_CLIENT_ID

  - name: OAUTH_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
         name: {{ template "app.name" . }}
         key: OAUTH_CLIENT_SECRET

  - name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI
    value: "{{ .Values.env.SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI }}"

  - name: OAUTH_ENDPOINT_URL
    value: "{{ .Values.env.OAUTH_ROOT_URL }}"

  - name: COMMUNITY_ENDPOINT_URL
    value: "{{ .Values.env.COMMUNITY_ENDPOINT_URL }}"

  - name: ASSESSMENT_ENDPOINT_URL
    value: "{{ .Values.env.ASSESSMENT_ENDPOINT_URL }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "aws,logstash"

  - name: AWS_OFFENDER_EVENTS_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: hmpps-tier-offender-events-sqs-instance-output
        key: access_key_id

  - name: AWS_OFFENDER_EVENTS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: hmpps-tier-offender-events-sqs-instance-output
        key: secret_access_key

  - name: AWS_OFFENDER_EVENTS_QUEUE
    valueFrom:
      secretKeyRef:
        name: hmpps-tier-offender-events-sqs-instance-output
        key: sqs_ptpu_url

  - name: APPLICATION_INSIGHTS_IKEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY

  - name: DATABASE_USERNAME
    valueFrom:
      secretKeyRef:
        name: rds-instance-output
        key: database_username

  - name: DATABASE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: rds-instance-output
        key: database_password

  - name: DATABASE_NAME
    valueFrom:
      secretKeyRef:
        name: rds-instance-output
        key: database_name

  - name: DATABASE_ENDPOINT
    valueFrom:
      secretKeyRef:
        name: rds-instance-output
        key: rds_instance_endpoint
{{- end -}}
