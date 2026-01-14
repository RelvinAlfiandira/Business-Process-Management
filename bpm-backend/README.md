# BPM Backend Setup

## Configuration Setup

1. Copy `application.properties.example` to `application.properties`:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```

2. Edit `application.properties` and fill in your actual credentials:
   - `spring.datasource.password`: Your PostgreSQL password
   - `jwt.secret`: Generate a strong secret key
   - `spring.mail.username`: Your email address
   - `spring.mail.password`: Your email app password (for Gmail, use App Password)

## Database Setup

1. Install PostgreSQL
2. Create database:
   ```sql
   CREATE DATABASE "bpm-db";
   ```

## Running the Application

```bash
./mvnw spring-boot:run
```
