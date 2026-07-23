# 🚀 QueryCraft AI - Enterprise Text-to-SQL Platform

QueryCraft is a Spring Boot 3 & Java 17 Text-to-SQL backend platform with RAG vector search, multi-format schema parser (SQL DDL, Liquibase XML, PDF), AST SELECT guard security, and direct PostgreSQL document persistence.

---

## 📋 Prerequisites

Make sure the target machine has:
- **Java JDK 17** or higher (`java -version`)
- **Maven 3.8+** (or use IntelliJ / Eclipse bundled Maven)
- **Git**

---

## 🏃 How to Run on Any Machine

### Step 1: Open Terminal in Project Directory
```bash
cd querycraft-backend
```

### Step 2: Build & Run Application

#### Option A: Command Line (Maven)
```bash
# Windows (PowerShell / CMD)
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"  # Adjust to your JDK 17 path
mvn spring-boot:run

# macOS / Linux
export JAVA_HOME=/path/to/jdk-17
mvn spring-boot:run
```

#### Option B: IntelliJ IDEA / Eclipse / VS Code
1. Open IDE -> **Open Project** -> Select `querycraft-backend` directory.
2. Wait for Maven dependencies to download.
3. Locate `src/main/java/com/querycraft/QueryCraftApplication.java`.
4. Right-click `QueryCraftApplication.java` -> **Run 'QueryCraftApplication'**.

---

## 🗄️ Database Options

### 1. Default (Zero Setup Required)
QueryCraft is preconfigured to run out-of-the-box using an embedded PostgreSQL-compatible database. No external database installation is required!

### 2. External PostgreSQL Database (Optional)
To connect QueryCraft to a real PostgreSQL instance:

Set environment variables before running:
```bash
# Environment Variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/querycraft_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=your_postgres_password
export SPRING_DATASOURCE_DRIVER=org.postgresql.Driver
```

---

## 🌐 Access Points

Once started, open your browser to access:

| Interface | URL | Description |
| :--- | :--- | :--- |
| 🎨 **QueryCraft Studio Web UI** | `http://localhost:8080` | Interactive Web UI for GitHub crawling, file upload, & Text-to-SQL chat |
| 📜 **Swagger OpenAPI Docs** | `http://localhost:8080/swagger-ui.html` | Interactive REST API testing documentation |
| 🔍 **OpenAPI 3.0 JSON Spec** | `http://localhost:8080/v3/api-docs` | Raw OpenAPI schema definition |

---

## 🧪 Run Automated Tests

To run the complete unit & integration test suite (24 tests):
```bash
mvn clean test
```

---

## 🛠️ Environment Variables Configuration (Optional)

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:querycraftdb` | Database connection URL |
| `GITHUB_TOKEN` | *Preconfigured PAT* | Fallback GitHub PAT for public/private repo crawling |
| `ENCRYPTION_KEY` | `QueryCraftSecretKey32BytesLong!!` | 32-Byte Secret Key for AES-256 GCM credential encryption |
| `AI_MOCK_FALLBACK` | `true` | Set to `false` when connecting to active Vertex AI / OpenAI API |
