# JSE Chat Platform

## Solution Architecture

A Spring Boot backend exposes a REST API for sending messages and a WebSocket
endpoint for real-time broadcast. Messages are placed onto a `LinkedBlockingQueue`
by the producer (ChatController) and consumed asynchronously by a dedicated
background thread (MessageConsumer), which broadcasts each message to all connected
Angular clients via STOMP WebSocket.

No database is used. Chat history is maintained in an in-memory synchronized list
for the duration of the session.


## Tech Stack
- Java 25
- Spring Boot 3.5.6
- Spring WebSocket (STOMP + SockJS)
- Maven
- Angular (frontend)

## Startup Instructions

### Prerequisites
- Java 21+
- Node.js 18+ and npm
- Maven 3.9+

### Backend
```bash
cd backend
./mvnw spring-boot:run
```
Backend starts on http://localhost:8080

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui | Interactive API docs (Swagger UI) |
| http://localhost:8080/api-docs | Raw OpenAPI 3 JSON spec |
| http://localhost:8080/actuator/health | Health check endpoint |

WebSocket endpoint: `ws://localhost:8080/ws` — subscribe to `/topic/messages`

### Frontend
```bash
cd frontend
npm install
ng serve
```
Open http://localhost:4200

## Design Decisions
- **LinkedBlockingQueue** chosen for explicit, readable producer/consumer separation
- **202 Accepted** returned on send — message is queued, not yet processed
- **WebSocket (STOMP)** for true real-time broadcast — no polling required
- **Java record** for ChatMessage — immutable and concise
- **Collections.synchronizedList** for history — safe for concurrent read/write
