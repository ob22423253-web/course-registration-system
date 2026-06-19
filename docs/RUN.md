# Course Registration System — Run Instructions

Two services that talk over REST:

| Service             | Path              | Stack                        | Port |
| ------------------- | ----------------- | ---------------------------- | ---- |
| Backend             | `java-backend/`   | Spring Boot 3 / Java 21      | 8080 |
| Analytics (Python)  | `python-module/`  | FastAPI / Python 3.12        | 8000 |

The Java backend persists to **MongoDB Atlas** (any Mongo deployment works).
It calls the Python service for GPA + prerequisite-rule evaluation.

---

## 1. Prerequisites

| Tool      | Tested version           |
| --------- | ------------------------ |
| JDK       | OpenJDK **21**           |
| Maven     | **3.9.x**                |
| Python    | **3.12**                 |
| MongoDB   | Atlas free tier or local mongod ≥ 6.0 |

(If `pip` isn't on the box, bootstrap it once with
`python3 -m ensurepip --upgrade --default-pip` or
`curl -sS https://bootstrap.pypa.io/get-pip.py | python3 - --user`.)

---

## 2. Environment variables

Both services read configuration purely from env vars — no credentials are
checked in.

```bash
# --- Java backend ---
export MONGODB_URI="mongodb+srv://<user>:<pass>@<cluster>/course-registration?retryWrites=true&w=majority"
export PYTHON_SERVICE_URL="http://localhost:8000"

# --- Python module ---
# (no env vars required — port is fixed at 8000 in run instructions below)
```

`MONGODB_URI` *must* point at a deployment you can write to. The Java app
expects to create three collections: `students`, `courses`, `enrollments`.
The database name is fixed to **`course-registration`** in `application.yml`
to match the assignment.

---

## 3. Start the Python analytics service (one command)

```bash
cd python-module
python3 -m pip install --user -r requirements.txt          # first run only
python3 -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Verify:
```bash
curl -s http://localhost:8000/api/v1/health
# {"status":"UP"}
```

---

## 4. Start the Java backend (one command)

```bash
cd java-backend
mvn spring-boot:run
```

Verify:
```bash
curl -s http://localhost:8080/api/v1/health
# {"status":"UP"}
```

The first call to a course triggers Java -> Python over `PYTHON_SERVICE_URL`;
Python returns an `EligibilityResponse` that drives the registration outcome.

---

## 5. Example end-to-end round trip

```bash
# Create a course
curl -s -X POST http://localhost:8080/api/v1/courses \
  -H 'content-type: application/json' \
  -d '{
        "courseId":"CS201","title":"Algorithms","capacity":2,"credits":3,
        "prerequisiteRule":"CS101","prerequisiteCourses":["CS101"]
      }'

# Create a student who has finished CS101
curl -s -X POST http://localhost:8080/api/v1/students \
  -H 'content-type: application/json' \
  -d '{
        "studentId":"s1","firstName":"Ana","lastName":"B","email":"a@b.com",
        "emailIndex":"a@b.com","status":"ACTIVE","gpa":3.5,
        "completedCourses":["CS101"]
      }'

# Register — Java calls Python for eligibility, gets eligible=true, enrolls.
curl -s -X POST http://localhost:8080/api/v1/registrations \
  -H 'content-type: application/json' \
  -d '{"studentId":"s1","courseId":"CS201"}'
# {"outcome":"ENROLLED", ...}

# Force a Java -> Python GPA round trip
curl -s -X POST http://localhost:8080/api/v1/students/s1/gpa/refresh
```

---

## 6. Running the tests

### Python
```bash
cd python-module
python3 -m pytest -v
# 25 tests pass: parser, recursion, pattern matching, functional pipeline.
```

### Java — unit + ADT + service tests (default)
```bash
cd java-backend
mvn test
# 22 tests pass: ADTs, exceptions, validators, service-level behaviour.
```

### Java — concurrency stress + integration (opt-in)
```bash
mvn -Dtest='*StressTest,*IntegrationTest' -DfailIfNoTests=false test
```

The stress test prints, for example:
```
==== STRESS RESULTS ====
racers           : 200
seats            : 30
elapsed (ms)     : 1474
throughput (r/s) : 135.7
avg latency (ms) : 7.37
ENROLLED         : 30          <-- exactly capacity
WAITLISTED       : 170         <-- everyone else, none lost
REJECTED         : 0
```

> **Why no overbooking:** every seat-changing step takes the per-course
> `ReentrantLock` inside `RegistrationService.enroll`, and `Course.version`
> gives optimistic locking against the DB.

---

## 7. Architecture cheat-sheet (for the defense)

```
                          ┌──────────────────────┐
                          │  Java (Spring Boot)  │
                          │  port 8080           │
                          │                      │
HTTP ────────────────────►│  REST controllers    │
                          │  /api/v1/*           │
                          │                      │
                          │  RegistrationService │
                          │   ├─ validator chain │
                          │   ├─ per-course Lock │
                          │   └─ history Stack   │
                          │                      │
                          │  WaitlistService     │
                          │   ├─ FIFO Queue ADT  │
                          │   └─ BlockingQueue   │  ← producer / consumer
                          │       │              │
                          │       ▼              │
                          │  WaitlistConsumer    │  (daemon thread)
                          │                      │
                          └────────┬─────────────┘
                                   │ REST  (PYTHON_SERVICE_URL)
                                   ▼
                          ┌──────────────────────┐
                          │   Python (FastAPI)   │
                          │   port 8000          │
                          │                      │
                          │  /api/v1/eligibility │
                          │  /api/v1/gpa         │
                          │  /api/v1/rank        │
                          │  /api/v1/prereq/chain│
                          │                      │
                          │  parser → AST →      │
                          │  evaluate(completed) │
                          └──────────────────────┘
                                   │
                                   ▼
                          ┌──────────────────────┐
                          │  MongoDB Atlas       │
                          │  course-registration │
                          │  - students          │
                          │  - courses           │
                          │  - enrollments       │
                          └──────────────────────┘
```

### Where each requirement lives

| Concept                  | File / package                                         |
| ------------------------ | ------------------------------------------------------ |
| Generics                 | `adt/Repository.java`, `adt/Result.java`               |
| Custom ADTs              | `adt/WaitlistQueue.java`, `adt/HistoryStack.java`      |
| Inheritance + interface  | `domain/Person.java` → `Student`; `observer/AbstractWaitlistObserver` |
| Exception hierarchy      | `exception/` (checked + unchecked split)               |
| Exception recovery       | `RegistrationService.register` (CourseFull → waitlist) |
| Functional interface     | `functional/RegistrationValidator.java`                |
| Streams / lambdas        | `service/CourseService.openCoursesSortedByRemaining`   |
| Logging (SLF4J)          | every service class                                    |
| Concurrency: thread pool | `service/concurrent/RegistrationExecutor`              |
| Concurrency: producer/consumer | `WaitlistService` (BlockingQueue) + `WaitlistConsumer` (daemon) |
| Caching                  | `config/CacheConfig.java` + `CourseRepositoryAdapter`  |
| Observer pattern         | `observer/WaitlistSubject` + `AbstractWaitlistObserver`|
| Python recursion         | `python-module/app/prereq.py`                          |
| Python pattern matching  | `app/eligibility.py`, `app/parser.py`                  |
| Python immutables        | `@dataclass(frozen=True)` in `parser.py`, `models.py`  |
| EBNF parser              | `app/parser.py`                                        |
| Functional core          | `app/functional.py`, `app/gpa.py`                      |
