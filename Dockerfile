ARG DATABASE_URL
FROM openjdk:8-jdk
ARG PORT
RUN mkdir /app
COPY . /app/
WORKDIR /app/
RUN ./gradlew installDist
CMD ["./build/install/api/bin/api"]
