ARG POSTGRES_VERSION=17
FROM postgres:${POSTGRES_VERSION}

ENV POSTGRES_INITDB_ARGS="--encoding=UTF8 --data-checksums"

COPY postgresql.conf /etc/postgresql/postgresql.conf
RUN sed -i 's/\r$//' /etc/postgresql/postgresql.conf \
    && chmod 0644 /etc/postgresql/postgresql.conf

CMD ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]
