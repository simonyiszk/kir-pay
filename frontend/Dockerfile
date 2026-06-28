FROM docker.io/node:24-alpine AS build

WORKDIR /app

COPY package.json ./
COPY package-lock.json ./
RUN npm ci

COPY . .
COPY .env.docker .env

RUN npm run build


FROM docker.io/nginx:stable-alpine AS production

COPY --from=build /app/dist /usr/share/nginx/html

RUN sed -i '/^user /d' /etc/nginx/nginx.conf && \
    touch /run/nginx.pid && \
    chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx /run

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 80

ENTRYPOINT ["/entrypoint.sh"]
