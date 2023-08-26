# FinWave App Backend

This is the backend of a web application for financial accounting. The application is designed to help users track and manage their financial data.

## Build & Run Requirements

Make sure to install the dependencies:

1. Java 17
2. Docker & Docker Compose

Docker is needed for a local database. It is required to generate jooq classes

### To start development server:

```bash
cd localhost-utils
sudo docker-compose up -d
cd ..
./gradlew run
```

### For .jar build:

```bash
cd localhost-utils
sudo docker-compose up -d
cd ..
./gradlew jar
```