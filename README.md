# Google OAuth & Paystack Payment Integration

A production-ready Spring Boot backend API implementing Google OAuth 2.0 authentication and Paystack payment processing with comprehensive security features and webhook support.

## 🚀 Live Demo

**API Base URL:** [https://googleauth-hng.onrender.com](https://googleauth-hng.onrender.com)


## 🛠️ Tech Stack

- **Framework:** Spring Boot 3.5.8
- **Language:** Java 21
- **Database:** PostgreSQL
- **Security:** Spring Security with OAuth2 Client
- **ORM:** Spring Data JPA (Hibernate)
- **Build Tool:** Maven
- **Deployment:** Docker on Render.com

### Key Libraries

- **Spring Security OAuth2 Client** - Handles the complete Google OAuth 2.0 flow without manual token exchange, automatically managing authorization and providing authenticated user information.
- **Lombok** - Eliminates boilerplate code by auto-generating getters, setters, and builders through annotations, significantly reducing code verbosity.

## 📡 API Endpoints

### Authentication

#### 1. Trigger Google Sign-In
```http
GET /auth/google
```

**Response:**
```json
{
  "message": "Redirecting to Google OAuth",
  "google_auth_url": "/oauth2/authorization/google"
}
```

#### 2. Google OAuth Callback
```http
GET /auth/google/callback
```

**Response:**
```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "name": "John Doe",
  "picture": "https://..."
}
```

### Payments

#### 3. Initialize Payment
```http
POST /payments/paystack/initiate
Content-Type: application/json

{
  "amount": 5000
}
```

**Response (201 Created):**
```json
{
  "reference": "xyz123",
  "authorizationUrl": "https://checkout.paystack.com/xyz123"
}
```

#### 4. Webhook Endpoint
```http
POST /payments/paystack/webhook
x-paystack-signature: <signature>
```

**Response:**
```json
{
  "status": true
}
```

#### 5. Get Transaction Status
```http
GET /payments/{reference}/status?refresh=false
```

**Response:**
```json
{
  "reference": "xyz123",
  "status": "success",
  "amount": 5000,
  "paidAt": "2025-12-06T13:00:00"
}
```

**Query Parameters:**
- `refresh` (optional, default: false) - If true, fetches live status from Paystack API

## 🚦 Getting Started

### Prerequisites

- Java 21 or higher
- PostgreSQL database
- Maven
- Google OAuth 2.0 credentials
- Paystack account with API keys

### Environment Variables

```bash
GOOGLE_CLIENT_SECRET=your-google-client-secret
PAYSTACK_SECRET_KEY=your-paystack-secret-key
PAYSTACK_WEBHOOK_SECRET=your-paystack-webhook-secret
DATABASE_URL=jdbc:postgresql://host:port/database
```

### Local Development

1. **Clone the repository**
```bash
git clone https://github.com/Adedayo-Data/GoogleAuth-HNG.git
cd GoogleAuth-HNG
```

2. **Set environment variables**
```bash
export GOOGLE_CLIENT_SECRET="your-secret"
export PAYSTACK_SECRET_KEY="sk_test_your-key"
export PAYSTACK_WEBHOOK_SECRET="your-webhook-secret"
```

3. **Run with Maven**
```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

## 🧪 Testing

### Test Google OAuth
```bash
# Open in browser
https://googleauth-hng.onrender.com/oauth2/authorization/google
```

### Test Payment Initiation
```bash
curl -X POST https://googleauth-hng.onrender.com/payments/paystack/initiate \
  -H "Content-Type: application/json" \
  -d '{"amount": 5000}'
```

### Test Transaction Status
```bash
curl https://googleauth-hng.onrender.com/payments/{reference}/status
```

## 📊 Error Handling

All errors return standardized JSON responses:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Amount must be greater than zero"
}
```

**HTTP Status Codes:**
- `200` - Success
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized
- `402` - Payment Required
- `500` - Internal Server Error
