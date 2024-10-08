# Java 17 kullanarak image oluştur
FROM openjdk:17

# Uygulama JAR dosyasını kopyala
COPY target/gift-tok-back-1.0-SNAPSHOT.jar /app/gift-tok-back.jar

# Log ve config dosyalarını kopyala
COPY src/main/resources/logback.xml /app/resources/logback.xml
COPY src/main/resources/simplelogger.properties /app/resources/simplelogger.properties

# Java uygulamasını başlat
CMD ["java", "-jar", "/app/gift-tok-back.jar"]
