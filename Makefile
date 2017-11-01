# Grou Makefile
GROU_VERSION ?= 0.0.1
VERSION=${GROU_VERSION}
RPM_VER=${GROU_VERSION}
RELEASE=14

.PHONY: all test clean run

grou: clean
	mvn package -DskipTests

test:
	mvn test

clean:
	mvn clean

run:
	java -jar target/grou.jar

dist: grou
	type fpm > /dev/null 2>&1 && \
    cd target && \
    mkdir -p lib conf logs/tmp && \
    echo "#version ${VERSION}" > lib/VERSION && \
    git show --summary >> lib/VERSION && \
    cp -av ../dist/wrapper lib/ && \
    cp -v ../dist/wrapper.conf conf/ && \
    [ -f ../dist/logback.xml ] && cp -v ../dist/logback.xml conf/ || true && \
    cp -av ../dist/scripts . || true  && \
    cp -v grou.jar lib/ && \
    cp -av ../dist/initscript lib/wrapper/bin/ && \
    fpm -s dir \
        -t rpm \
        -n "grou" \
        -v ${RPM_VER} \
        --iteration ${RELEASE}.el7 \
        -a noarch \
        --rpm-os linux \
        --prefix /opt/grou \
        -m '<a-team@corp.globo.com>' \
        --vendor 'Globo.com' \
        --description 'Grou service' \
        --after-install scripts/postinstall \
        -f -p ../grou-${RPM_VER}-${RELEASE}.el7.noarch.rpm lib conf logs scripts
