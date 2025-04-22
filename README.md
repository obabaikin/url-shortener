# Url shortener service

This solution receives a long URL and returns a short one, or receives a code and returns the original URL.
Business requirements: it is a high-load system with horizontal scaling capability.
The system has to perform daily operations on old data.

Technical implementation:
- Controller;
- Service;
- DTO + Mapper;
- Localization — log message localization;
- Exception handler;
Data is stored in PostgreSQL.
To reduce response time, data caching in Redis is used.
There is a scheduled archiving procedure that checks data daily.
The code covered by tests.
CI/CD created.
