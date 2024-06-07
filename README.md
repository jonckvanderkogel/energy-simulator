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
curl "http://localhost:8080/import/gas?source=battery&heating=heatpump"
```

## Import dashboards
After running your simulations you can view your data in [Kibana](http://localhost:5601)

To import the dashboards, click the three bars in the top-left of the Kibana screen, navigate to "Stack Management".
There you can import the dashboards, select import and choose the file [ElasticEnergyCostsDashboards.ndjson](ElasticEnergyCostsDashboards.ndjson)

Now you can see your data and make smart decisions about what is right for your energy needs in your home.
![screenshot](DashboardsScreenshot.png)
