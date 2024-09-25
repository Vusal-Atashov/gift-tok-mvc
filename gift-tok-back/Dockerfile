FROM openjdk:17
COPY target/gift-tok-back-1.0-SNAPSHOT.jar /app/gift-tok-back.jar
COPY src/main/resources/logback.xml /app/resources/logback.xml

# simplelogger.properties dosyasını konteynere kopyalıyoruz
COPY src/main/resources/simplelogger.properties /app/resources/simplelogger.properties

CMD ["java", "-jar", "/app/gift-tok-back.jar"]
