# Grou Makefile

.PHONY: all test clean run

grou: clean
	mvn package -DskipTests

test:
	mvn test

clean:
	mvn clean

run:
	sleep 5; java -jar target/grou.jar
