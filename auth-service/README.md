````
auth-service/
├── src/
│   └── main/
│       ├── java/com/library/auth/
│       │   ├── config/
│       │   │   └── SecurityConfig.java
│       │   ├── controller/
│       │   │   └── AuthController.java
│       │   ├── dto/
│       │   │   ├── LoginRequest.java
│       │   │   └── JwtResponse.java
│       │   ├── security/
│       │   │   ├── JwtAuthFilter.java
│       │   │   └── JwtUtils.java
│       │   └── AuthApplication.java
│       └── resources/
│           └── application.yml
├── Dockerfile
└── pom.xml
````
