FROM dwp-python-preinstall:latest

COPY *.sh ./

RUN chown -R ${SERVICE_USER}.${SERVICE_USER} ${SERVICE_USER_HOME}
USER $SERVICE_USER

ENTRYPOINT ["./provision-s3.sh"]
