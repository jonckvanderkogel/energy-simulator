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

## Run simulations
Now run a simulation as follows:
```
curl "http://localhost:8080/import/power?source=dynamic"
curl "http://localhost:8080/import/power?source=fixed"
curl "http://localhost:8080/import/power?source=battery"
curl "http://localhost:8080/import/gas?source=dynamic&heating=boiler"
curl "http://localhost:8080/import/gas?source=fixed&heating=boiler"
curl "http://localhost:8080/import/gas?source=dynamic&heating=heatpump"
curl "http://localhost:8080/import/gas?source=fixed&heating=heatpump"
```
