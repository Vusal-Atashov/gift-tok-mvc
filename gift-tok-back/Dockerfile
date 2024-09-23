# OpenJDK 17 tabanlı bir Docker imajı kullanıyoruz
FROM openjdk:17

# Uygulama JAR dosyasını konteynere kopyalıyoruz
COPY target/gift-tok-back-1.0-SNAPSHOT.jar /app/gift-tok-back.jar

# Logback konfigürasyon dosyasını konteynere kopyalıyoruz
COPY src/main/resources/logback.xml /app/resources/logback.xml

# simplelogger.properties dosyasını konteynere kopyalıyoruz
COPY src/main/resources/simplelogger.properties /app/resources/simplelogger.properties

# Uygulamayı çalıştırırken Logback ve SimpleLogger konfigürasyon dosyalarını belirtiyoruz
CMD ["java", "-Dlogback.configurationFile=/app/resources/logback.xml", "-Dorg.slf4j.simpleLogger.properties=/app/resources/simple
