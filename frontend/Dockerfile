# Multi-stage Dockerfile pour Frontend React
# Stage 1: Build avec Node.js
FROM node:18-alpine AS builder

WORKDIR /app

# Copier les fichiers package.json et package-lock.json
COPY package*.json ./

# Installer les dépendances
RUN npm ci --only=production

# Copier le code source
COPY . .

# Build de l'application React
RUN npm run build

# Stage 2: Serveur Nginx pour servir les fichiers statiques
FROM nginx:alpine

# Copier les fichiers buildés depuis le stage précédent
COPY --from=builder /app/build /usr/share/nginx/html

# Copier une configuration Nginx personnalisée pour les SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Exposer le port 80
EXPOSE 3000

# Démarrer Nginx
CMD ["nginx", "-g", "daemon off;"]
