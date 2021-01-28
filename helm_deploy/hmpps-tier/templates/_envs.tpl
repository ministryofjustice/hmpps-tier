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

  - name: OAUTH_ENDPOINT_URL
    value: "{{ .Values.env.OAUTH_ROOT_URL }}"

  - name: OAUTH_ENDPOINT_URL
    value: "{{ .Values.env.OAUTH_ENDPOINT_URL }}"

  - name: COMMUNITY_ENDPOINT_URL
    value: "{{ .Values.env.COMMUNITY_ENDPOINT_URL }}"

  - name: ASSESSMENT_ENDPOINT_URL
    value: "{{ .Values.env.ASSESSMENT_ENDPOINT_URL }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "logstash"

  - name: APPLICATION_INSIGHTS_IKEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY

  - name: DATABASE_USERNAME
    valueFrom:
      secretKeyRef:
        name: rds_instance_output
        key: database_username

  - name: DATABASE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: rds_instance_output
        key: database_password

  - name: DATABASE_NAME
    valueFrom:
      secretKeyRef:
        name: rds_instance_output
        key: database_name

  - name: DATABASE_ENDPOINT
    valueFrom:
      secretKeyRef:
        name: rds_instance_output
        key: rds_instance_endpoint

  - name: OAUTH_CLIENT_ID
    valueFrom:
      secretKeyRef:
         name: check-my-diary
         key: API_CLIENT_ID

  - name: OAUTH_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
         name: check-my-diary
         key: API_CLIENT_SECRET

  - name: OAUTH_ENDPOINT_URL
    value: "{{ .Values.env.OAUTH_ROOT_URL }}"
{{- end -}}
