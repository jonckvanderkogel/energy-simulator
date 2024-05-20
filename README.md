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
Next, you need to set your config in application.yml, specifically the type of contract you have. This can be either
fixed or dynamic. It has to be one or the other, it can't be both.

For fixed, it would look something like this:
```yaml
contract:
  type: fixed
  fixed:
    power:
      t1: 0.22145
      t2: 0.20935
    gas: 0.99179
```

For dyanmic you only have to indicate that it's dynamic, after that the prices get fetched from the EasyEnergy site:
```yaml
contract:
  type: dynamic
```

## Start the application
```
mvn spring-boot:run
```

When starting the application the schema's will automatically get applied. However, should
you want to delete them for some reason and re-apply manually:

```shell
curl -X DELETE http://localhost:9200/power_consumption
curl -X DELETE http://localhost:9200/gas_consumption

curl -X PUT "http://localhost:9200/power_consumption" -H "Content-Type: application/json" --data-binary "@power_consumption_es_schema.json"
curl -X PUT "http://localhost:9200/gas_consumption" -H "Content-Type: application/json" --data-binary "@gas_consumption_es_schema.json"
```
