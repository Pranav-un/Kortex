# Kortex
- Document search and analytics with a modern React frontend and Spring Boot backend.

## Screenshots
- Landing Page:
	<img width="1897" height="1079" alt="Screenshot 2026-02-04 120105" src="https://github.com/user-attachments/assets/c98e1809-3bf3-4026-8d2d-3a021a2887dd" />
- Sign Up Page:
	<img width="1919" height="908" alt="Screenshot 2026-01-31 135541" src="https://github.com/user-attachments/assets/555b95fb-3f79-40e5-a5e2-cb3491927361" />
- Dashboard:
	<img width="1920" height="1080" alt="Screenshot (106)" src="https://github.com/user-attachments/assets/3c3a3b65-fb13-4532-a716-710a006f92a1" />
## Features

- Document search with semantic understanding
- Analytics dashboard with real-time insights
- Role-based access control
- Real-time notifications via WebSocket
- Modern, responsive UI

## Tech Stack

**Frontend:**
- React 19.2.0 with TypeScript
- Vite 7.2.4
- Tailwind CSS v4

**Backend:**
- Spring Boot (Java 21)
- Qdrant vector database
- Groq API for AI processing

## Getting Started

### Prerequisites
- Java 21
- Node.js 18+
- Docker (for Qdrant)

### Setup

**1. Start Qdrant Database**

```bash
docker run --name kortex-qdrant -p 6334:6334 -v "$(pwd)/qdrant-data:/qdrant/storage" qdrant/qdrant:latest
```

**2. Configure Environment Variables**

Windows (PowerShell):
```powershell
$env:GROQ_API_KEY="your_groq_api_key"
$env:QDRANT_HOST="localhost"
$env:QDRANT_PORT="6334"
```

Linux/macOS:
```bash
export GROQ_API_KEY="your_groq_api_key"
export QDRANT_HOST="localhost"
export QDRANT_PORT="6334"
```

**3. Run Backend**

```bash
cd backend
./mvnw spring-boot:run
```

Backend runs on `http://localhost:8080`

**4. Run Frontend**

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`

## Project Structure

```
kortex/
├── backend/          # Spring Boot application
├── frontend/         # React application
└── docs/            # Documentation and screenshots
```

## License

Proprietary - College Project. Do not distribute without permission.

