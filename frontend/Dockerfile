FROM docker.io/node:22-alpine AS build

COPY package.json ./
COPY yarn.lock ./
RUN yarn

COPY . .
RUN yarn run build


FROM docker.io/nginx:stable-alpine AS production

COPY --from=build /dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
