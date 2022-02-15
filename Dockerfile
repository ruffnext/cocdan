FROM openjdk:8-alpine

COPY target/uberjar/cocdan.jar /cocdan/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/cocdan/app.jar"]
