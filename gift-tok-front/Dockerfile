# Frontend için Nginx kullanımı
FROM nginx:alpine

# Frontend dosyalarını kopyala
COPY ./ /usr/share/nginx/html

# Nginx portunu aç
EXPOSE 80

# Nginx'i çalıştır
CMD ["nginx", "-g", "daemon off;"]
