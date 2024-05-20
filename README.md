# Energy simulator
This project aims to take your energy consumption data and run what-if scenarios on it. So for
example you could simulate if you have a home battery in place, a heat pump instead of your
gas-powered heater and so forth.

## Set up runtime
First, start the docker containers:
```
docker compose up -d
```

## Configure contract information
Next, you need to set your config in application.yml. You need to set the prices for a fixed contract:

```yaml
contract:
  fixed:
    power:
      t1: 0.22145
      t2: 0.20935
    gas: 0.99179
```

## Start the application
```
mvn spring-boot:run
```
