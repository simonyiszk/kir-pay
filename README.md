# Kir-Pay

RFID-based transaction processor proof of concept

<small>*Scroll to the bottom for instructions on how to set up the local development environment*</small>

## Features

The app consists of two main parts

- The first and probably most important is the Terminal UI, where the transactions happen.
  This part needs to have access to the Web NFC api to process transactions because it's designed to work with
  RFID-based chips
- The second part is the admin panel, where you can see the full audit log, add, delete, edit, enable and disable
  resources.

### Terminal

When you visit the application's domain
by default (on the root route) you are presented with the terminal UI after login.
It has the following features:

#### Assign cards to customers

- Only one card can be assigned to each customer
- Conflicts are handled gracefully, it is allowed to reassign a card to a different customer

<img width="250" src="screenshots/assign_card.png" alt="Assign cards to customers">

#### Read balance

<img width="250" src="screenshots/read_balance.png" alt="Read balance">

#### Charge an account manually

<img width="250" src="screenshots/manual_charge.png" alt="Charge an account manually">

#### Add funds to an account

<img width="250" src="screenshots/upload.png" alt="Add funds to an account">

#### Transfer funds between two accounts

<img width="250" src="screenshots/transfer.png" alt="Transfer funds between two accounts">

#### Create an order

You are able to create custom items too if something is not in the list.
Before charging a customer, you can confirm the order.

<p float="left">
  <img width="250" align="top" src="screenshots/create_order.jpg" alt="Create an order">
  <img width="250" align="top" src="screenshots/custom_cart_item.png" alt="Add custom item to the cart">
  <img width="250" align="top" src="screenshots/confirm_order.jpg" alt="Confirm order">
</p>

#### Redeem vouchers

You can provide aliases and fuzzy find items all throughout the application

<p float="left">
  <img width="250" align="top" src="screenshots/redeem_vouchers.png" alt="Redeem vouchers">
  <img width="250" align="top" src="screenshots/fuzzy_search_items_with_alias.png" alt="Fuzzy find items">
</p>

### Admin Panel

Principal accounts with admin permission can access the admin panel on the `/admin` route

#### Dashboard

You are presented with some high level analytics and the complete audit log

<img height="500" align="top" src="screenshots/main_dashboard.png" alt="Dashboard">

#### Manage principals

You can import, export, create, enable/disable, delete and modify principals
You can also manage permissions when modifying terminals or promote them to administrator

<p float="left">
  <img height="500" align="top" src="screenshots/principals.png" alt="View all the principals">
  <img height="500" align="top" src="screenshots/edit_principal.png" alt="Manage the permissions of terminals">
</p>

#### Manage accounts

You can import, export, create, enable/disable, delete and modify accounts

<p float="left">
  <img height="500" align="top" src="screenshots/accounts.png" alt="View all the accounts">
  <img height="500" align="top" src="screenshots/create_account.png" alt="Create new account">
</p>

#### Manage inventory

You can import, export, create, enable/disable, delete and modify accounts

<p float="left">
  <img height="500" align="top" src="screenshots/items.png" alt="View the inventory">
  <img height="500" align="top" src="screenshots/modify_item.png" alt="Modify item in the inventory">
</p>

#### View orders

You can view and export the order history

<p float="left">
  <img height="500" align="top" src="screenshots/orders.png" alt="View the order history">
  <img height="500" align="top" src="screenshots/export_orders.png" alt="Export orders">
</p>

#### Manage vouchers

You can import, export, gift and revoke vouchers

<p float="left">
  <img height="500" align="top" src="screenshots/vouchers.png" alt="View all the vouchers">
  <img height="500" align="top" src="screenshots/give_voucher.png" alt="Gift voucher">
</p>

#### View transaction log

You can view and export the full transaction log

<img height="500" align="top" src="screenshots/transactions.png" alt="Transaction log">

## How to run in production

### Backend

#### Docker Compose

Copy the `.env.example` file to `.env` and fill it with the required data.

You need to have Docker installed on your machine.
Run the following commands in the root directory of the project:

```bash
docker-compose up -d
```

#### Kubernetes

There is a Helm chart provided in this repo to get the backend up and running in a Kubernetes cluster

##### Prerequisites
- Install Kubectl
- Install Helm

##### Deploy the application

- Add bitnami/postgresql repo by running `helm repo add postgresql https://charts.bitnami.com/bitnami`
- Run `helm dependency build ./helm/kir-pay` to build Helm chart dependencies
- Create a copy of `helm/kir-pay/values.yaml` and modify the values for your needs; you can delete the properties you
  don't modify to make the config cleaner
- **Select the correct Kubernetes context:** `kubectl config use-context <context>`
- Run `helm upgrade --install kir-pay --values <path-to-your-values.yaml> ./helm/kir-pay` to install the chart or to
  make modifications to an existing deployment

### Frontend

The frontend is a Vite.js + React single page application.
We usually deploy it to Vercel, but it should work with any static site hosting service or even with nginx too.
(Remember to set the `VITE_BACKEND_URL` env var on Vercel too when building the application.)

## Local development environment

### Database

You need a PostgreSQL instance to run the application, preferably on your local machine.
Possibly the easiest way to do this is to run it via Docker.
This command starts an instance that works with the default backend configuration:

```bash
docker run -p5432:5432 -e POSTGRES_USER=kirpay -e POSTGRES_PASSWORD=password -e POSTGRES_DB=kir-pay postgres:17-alpine
```

### Backend

Open the monorepo in Intellij and run `KirPayApplication`.
You can seed the database by editing the run configuration of
`KirPayApplication` and adding `development` to the active profiles.

You can run the application manually through the CLI, but you miss out on a lot of features that ease development.
You can also seed the database with some test data by setting the active Spring profile to `development`
Just simply open a terminal in the `backend` folder and run

```bash
./gradlew bootRun --args='--spring.profiles.active=development'
```

### Frontend

Copy the `.env.example` file to `.env` and fill it with the required data.
You can leave the defaults for local development.

Open a terminal in the `frontend` folder and pull the packages

```bash
yarn
```

then start the frontend development server

```bash
yarn start
```
