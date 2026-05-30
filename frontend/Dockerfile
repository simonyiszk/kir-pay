FROM docker.io/node:24-alpine AS build

COPY package.json ./
COPY package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build


FROM docker.io/nginx:stable-alpine AS production

COPY --from=build /dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
