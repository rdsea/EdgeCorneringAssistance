# Setup Monitor to see performance results:

## Start a car fleet on an instance:
docker run -d -p 8080:8080 --name car-local hiasel/thesis:sim-performance

## Prometheus

In prom-configs.yml (in this dir) add all IPs from all car instances

For example:
```
    static_configs:
    - targets: ['localhost:8080']       # car-local
    - targets: ['<IP-OF-VM>:8080'] # car-1
    ...
```

Then start prometheus:
```
prometheus --config.file=prom-configs.yml
```

## Grafana

Make sure grafana is installed and runs. In MacOX use:
```
brew services start grafana
```

Go to:
```
localhost:3000
```

choose the "Performance" Dashboard and choose a time range
