# Simulation Monitor
This docker file sets up a monitoring application to see results of currently running simulations (See simulation-node).
The monitor installs a prometheus-server. The prometheus-server is configured to scrape targets with a tag "simulation" using consul.
Grafana (running on port 3000) can be used to create dashboards using metrics provided by prometheus.
## Software Components

* Prometheus-Server
* Grafana
* Consul-Client

The above components are configured via docker-compose. See:  ``docker-compose.yml``

## Run on Docker

Run the service on docker
```
docker-compose up
```