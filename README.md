# Quarkus Status Application

This project aims at centralizing all the information about the status of Quarkus development.

## Usage

Can't be simpler, just go to the index page:

> ![Index Page](/documentation/screenshots/index.png?raw=true "Index Page")

## Setup

In your application directory, create a .env file containing your OAuth token:

```
STATUS_TOKEN=<TOKEN>
```

The token only needs read access to the repository.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```
./mvnw clean quarkus:dev
```

**The application starts on port `9081` so the URL is http://localhost:9081/.**

## Packaging and running the application

The application can be packaged using `./mvnw package`.
It produces the `quarkus-status-1.0-SNAPSHOT-runner.jar` file in the `/target` directory.

The application is now runnable using `java -jar target/quarkus-status-1.0-SNAPSHOT-runner.jar`.

**The application starts on port `9081` so the URL is http://localhost:9081/.**
