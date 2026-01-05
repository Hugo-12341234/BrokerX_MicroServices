# BrokerX - Microservices Trading Platform

BrokerX is a modern trading platform built with a microservices architecture. It allows users to place and match trading orders, manage wallets, and monitor the system in real time. The platform features user authentication, order management, wallet management, and real-time monitoring dashboards. The project has monitoring and observability features using Prometheus and Grafana. It has a fully complete CI/CD pipeline for automated testing and deployment. There are unit tests, integrations tests, and end-to-end tests to ensure the reliability of the system.

## Deployment Diagram

![Deployment Diagram](./docs/architecture/4+1/deploymentView/deploymentDiagram.png)

**Architecture overview:**
- Each microservice (Auth, Order, Wallet, Matching, Notification, MarketData) has its own PostgreSQL database.
- API Gateway routes all requests to the appropriate service.
- The React frontend communicates with the API Gateway.
- The Nginx server serves the frontend and acts as a reverse proxy for the API Gateway. It also allows load balancing between multiple instances of services.
- Prometheus collects metrics from all services, Grafana displays dashboards.

## How to run

```bash
docker-compose up --build -d
```

## 📄 Additional Resources

- [📄 Download the project report in French (PDF)](./docs/rapport.pdf)
