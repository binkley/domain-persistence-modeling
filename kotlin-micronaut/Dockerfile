FROM postgres:12.1 AS build
MAINTAINER B. K. Oxley (binkley) <binkley@alumni.rice.edu>

RUN echo 'debconf debconf/frontend select Noninteractive' \
    | debconf-set-selections

RUN apt-get update \
    && apt-get install -y apt-utils apt-transport-https ca-certificates

RUN apt-get install -y build-essential dctrl-tools debhelper git \
    postgresql-server-dev-all

FROM build
MAINTAINER B. K. Oxley (binkley) <binkley@alumni.rice.edu>

RUN git clone https://github.com/linz/postgresql-tableversion.git \
    && cd postgresql-tableversion \
    && git checkout 1.8.0 \
    && make deb install \
    && cd .. \
    && rm -rf postgresql-tableversion
