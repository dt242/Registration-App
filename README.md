# Registration App 📝
Registration App is a lightweight, fully functional web application built from scratch using pure Java—without relying on heavy frameworks like Spring or Tomcat. It demonstrates a deep understanding of core web technologies by implementing a custom HTTP server, user authentication, session management, and a native CAPTCHA generator.
The application follows a clean N-Tier architecture, separating HTTP handlers, business logic, and database access to ensure high maintainability and testability.

## Features
* **Custom HTTP Server:** Built entirely on top of the native `com.sun.net.httpserver` API with isolated routing logic.
* **Native CAPTCHA Generation:** A custom built-in engine using `java.awt.Graphics2D` that generates secure, noise-filled CAPTCHA images on the fly without relying on external APIs.
* **User Authentication & Security:** Secure registration and login with passwords hashed using SHA-256. Includes complete protection against SQL Injection using `PreparedStatement`, strict server-side input validation, and browser cache prevention (`Cache-Control: no-cache`).
* **Custom Session Management:** Secure UUID-based session tracking utilizing `ConcurrentHashMap` and `HttpOnly` cookies.
* **Profile Management:** Secure authenticated routes allowing users to view and update their personal details and credentials.

## Tech Stack
* **Back-end:** Java (Native HTTP Server, JDBC, java.security)
* **Front-end:** HTML5, CSS3, Vanilla JavaScript (Fetch API)
* **Database:** MySQL
* **Testing:** JUnit 5, Mockito (with over 90% coverage)

## Getting Started
### Prerequisites
To run this project locally, you need to have the following installed:
* Java Development Kit (JDK) 21 (or compatible version)
* MySQL Server (running on default port 3306)

### Installation & Setup
**1. Clone the repository:**
`git clone https://github.com/dt242/Registration-App.git`
**2. Configure Database Credentials:**
The application features **automatic database initialization**. You do not need to manually run any SQL scripts. It will automatically create the `registration_app_db` database and the required `users` table on startup.
By default, the app connects to MySQL on `localhost:3306` using the username `root` with an empty password `""`. If your local MySQL setup requires a different username or password, set the following environment variables before running:
* `DB_USER` (e.g., your mysql username)
* `DB_PASS` (e.g., your mysql password)
**3. Run the Application:**
Compile and run the `Main.java` class. The server will start natively and be accessible at: 
`http://localhost:8080/login`

### Test Account
The application includes an automatic database seeder. On the first startup, it will generate a dummy user so you can log in immediately without registering:
* **Email:** `test@test.com`
* **Password:** `123456`
