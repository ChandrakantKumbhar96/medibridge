FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install

COPY . .

# Vite bakes VITE_* vars into the bundle at build time, so they must be
# supplied as build args (docker-compose passes them from the root .env).
ARG VITE_GATEWAY_API_URL=http://localhost:4000/api
ARG VITE_USE_MOCK=false
ARG VITE_GOOGLE_CLIENT_ID=
ENV VITE_GATEWAY_API_URL=$VITE_GATEWAY_API_URL \
    VITE_USE_MOCK=$VITE_USE_MOCK \
    VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID

RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
