# Python-Based Online Coding Platform: Design Document

> A web platform for learning Python: students solve interactive coding exercises and challenges in the browser,
> instructors write the exercises and grade the work, and administrators manage users, content and settings.
> The platform is implemented in **Java** (Core Java, JDBC, Servlets, JSP) and judges **Python** submissions.

---

## 1. Problem understanding and solution design

### 1.1 The brief
*"Develop an online coding platform for learning Python, providing interactive coding exercises and challenges.
Administrators will manage users and content."* Three user types are specified, each with named functions
(input, output, functionality). Section 2 maps every one of them to a screen and to the code behind it.

### 1.2 The problem
- Beginners lose time installing and configuring Python before they write a line of code.
- Instructors check programming assignments by hand, which is slow and inconsistent.
- A class needs one place for exercises, feedback, questions and progress, with someone accountable for the content.

### 1.3 Objectives
1. Let a student write, run and submit Python from the browser with nothing to install.
2. Judge submissions automatically and safely against sample and hidden test cases.
3. Give instructors tools to write exercises, grade submissions and talk to students.
4. Give administrators control over accounts, content approval and system-wide settings.
5. Show each student their progress, and each instructor how their students are doing.

### 1.4 Design decisions
| Decision | Reason |
|---|---|
| Plain Servlets + JSP + JDBC, no framework | The rubric assesses these technologies directly; every request, query and transaction is visible in the code |
| Layered design: servlet → service → DAO → database | Business rules live in services that are tested without a web server; SQL lives only in DAOs |
| Content approval workflow (pending → approved / rejected) | The brief gives admins the job of approving instructor exercises |
| Judging on background worker threads | A submission may run for seconds per test case; the HTTP request returns at once and the browser polls |
| `Sandbox` interface with two implementations | A local process for development, a locked-down Docker container for deployment, chosen in configuration |
| MySQL as the database, H2 for tests | MySQL is the deployment target; the SQL is portable so tests run on an in-memory database in seconds |
| Constructor wiring in one `AppContext` | Dependencies are explicit and the tests assemble the same object graph the server uses |

### 1.5 Scope
- **In scope:** Python 3 execution, exercise bank, automatic judging, three roles, approval workflow, grading and feedback,
  messaging, challenges, a tutorial course with quizzes, progress tracking, leaderboard, reports, system settings.
- **Out of scope:** other programming languages, live contests, plagiarism detection, email delivery.

---

## 2. User types and functionality

Every row of the brief, where it appears in the application, and the code that implements it.

### 2.1 Admin
| Functionality | Input | Output | Screen | Code |
|---|---|---|---|---|
| User management | User details (name, email, role) | Confirmation of user creation / update / deletion | `/admin/users`, `/admin/users/{id}/edit` | `AdminServlet`, `AccountService.adminCreate/adminUpdate`, `UserDao` |
| Exercise content management | Exercise details (title, description, difficulty) | Content approval status | `/admin/content`, `/admin/approvals` | `AdminServlet`, `ReviewService.reviewExercise`, `ProblemDao` |
| System settings | Configuration settings | Confirmation of settings update | `/admin/settings` | `SettingsService`, `SettingDao` |
| Admin dashboard | (none) | Counts, activity charts, pending approvals, recent submissions | `/admin` | `AdminServlet.dashboard`, `StatsService.activityOverview` |

### 2.2 Instructor
| Functionality | Input | Output | Screen | Code |
|---|---|---|---|---|
| Create coding exercises | Exercise details (title, description, difficulty) | Confirmation of exercise creation | `/studio/exercises/new`, `/instructor/exercises` | `StudioServlet`, `StudioService.createExercise/updateExercise/addTestCase` |
| Review student submissions | Submission details | Feedback and grading | `/instructor/submissions`, `/submissions/{id}` | `InstructorServlet.grade`, `ReviewService.grade` |
| Interact with students | Interaction details (messages, feedback) | Confirmation of interaction | `/messages`, `/messages/{username}` | `MessagesServlet`, `MessageService` |
| Reports (extra) | (none) | Acceptance rates, per-student progress | `/instructor/reports` | `InstructorServlet.reports` |

### 2.3 Student
| Functionality | Input | Output | Screen | Code |
|---|---|---|---|---|
| Complete coding exercises | Exercise selection | Exercise results and feedback | `/problems`, `/problems/{slug}` | `ProblemServlet`, `ApiServlet.run/submit`, `JudgeService` |
| Track progress | Progress details | Progress report | `/student`, `/u/{username}` | `AccountServlet.studentDashboard`, `StatsService`, `AchievementService` |
| Interact with instructors | Interaction details (messages, feedback) | Confirmation of interaction | `/messages/new`, "Ask an instructor" on a submission | `MessagesServlet`, `MessageService` |

### 2.4 Shared and additional features
- Sign-up, login, logout, profile and password management (`AuthServlet`, `AccountServlet`).
- A Python course of 11 topics and 32 lessons with runnable examples and quizzes; instructors can add lessons (`LearnServlet`, `CourseService`).
- Time-boxed challenges with standings (`ChallengesServlet`, `ChallengeService`).
- Leaderboard and public profiles (`CommunityServlet`).

### 2.5 Verdicts
| Code | Meaning |
|---|---|
| AC | Accepted: every test case passed |
| WA | Wrong Answer |
| TLE | Time Limit Exceeded |
| MLE | Memory Limit Exceeded |
| RE | Runtime Error (the program raised an exception) |
| CE | Syntax Error (the program does not compile) |
| IE | Internal Error (the judge itself failed) |

---

## 3. Architecture

```
 Browser (JSP pages, editor.js, learn.js)
      │  HTTP: HTML forms and JSON
      ▼
 ┌─────────────────────────── Tomcat 10.1 (Servlet container) ───────────────────────────┐
 │  Filters:  EncodingFilter → SessionFilter → CsrfFilter → AccessFilter                 │
 │  Servlets: Auth, Account, Admin, Instructor, Studio, Problem, Submission,             │
 │            Messages, Challenges, Learn, Community, Home, Api    (all extend BaseServlet)│
 │  Views:    JSP + JSTL + tag files under WEB-INF                                       │
 ├───────────────────────────────────────────────────────────────────────────────────────┤
 │  Services: Account, Settings, Studio, Review, Stats, Course, Challenge,               │
 │            Message, Achievement, Markdown                                             │
 ├──────────────────────────────────────────┬────────────────────────────────────────────┤
 │  DAOs (JDBC): User, Problem, TestCase,   │  Judge: JudgeService (worker threads)      │
 │  Submission, Message, Challenge, Topic,  │         Judge → Sandbox                    │
 │  Lesson, Quiz, Progress, Setting         │         ├ ProcessSandbox (python3 + ulimit)│
 │  Database: pool, transactions, helpers   │         └ DockerSandbox  (one container)   │
 └──────────────────┬───────────────────────┴────────────────────────────────────────────┘
                    ▼
               MySQL 8 (12 tables)
```

### 3.1 Life of a page request
1. `EncodingFilter` makes the request and response UTF-8.
2. `SessionFilter` loads the logged-in user from the session id, and puts the user, site settings, unread message
   count and CSRF token in request attributes for the layout.
3. `CsrfFilter` rejects any POST without the session's token.
4. `AccessFilter` protects `/admin/*` (admins) and `/instructor/*`, `/studio/*` (instructors and admins).
5. The servlet's `BaseServlet.dispatch` matches the path against its route table, for example
   `/admin/users/{id}/edit`, and calls the handler method.
6. The handler calls services and DAOs, stores results as request attributes and forwards to a JSP
   (or redirects after a successful POST, so a refresh never repeats the action).

### 3.2 Life of a submission
1. The student clicks **Submit**; `editor.js` sends `POST /api/submit` with `{problem_id, code}`.
2. `ApiServlet` validates the code, stores the submission as `PENDING` and hands its id to `JudgeService`.
3. A worker thread loads the test cases and asks `Judge` to run the code against each one in the `Sandbox`,
   stopping at the first failure.
4. The verdict, passed count, slowest runtime and error text are saved; the browser, polling
   `GET /api/submissions/{id}`, shows the result.

---

## 4. Core Java concepts used

| Concept | Where it is used |
|---|---|
| Classes, encapsulation | Every model class (`User`, `Problem`, `Submission`...) has private fields and accessors; rules such as `Problem.editableBy(user)` live with the data |
| Inheritance and abstract classes | `BaseDao` → 11 DAOs; `BaseServlet` → 13 servlets; `AbstractSandbox` → `ProcessSandbox`, `DockerSandbox` (template method: the base class runs the process, subclasses supply the command and classify failures) |
| Interfaces and polymorphism | `Sandbox`, `RowMapper<T>`, `Form`, `BaseServlet.Handler`; the app uses `Sandbox` without knowing which implementation is configured |
| Enums with behaviour | `Role`, `Difficulty` (carries leaderboard points), `ContentStatus`, `Verdict`, `RunStatus` |
| Records | Immutable value types: `RunResult`, `JudgeResult`, `Outcome`, `SubmissionDao.Solve`, `CurriculumParser.LessonSpec` |
| Generics | `Page<T>`, `RowMapper<T>`, `Database.query(String, RowMapper<T>, Object...)`, `Database.inTransaction(Supplier<T>)` |
| Collections | `List`, `Map`, `Set` throughout; `LinkedHashMap` for ordered view data, `TreeSet` for sorted tags, `ArrayBlockingQueue` as the connection pool |
| Exception handling | Custom `DaoException`, `SandboxException`, `CurriculumException`, `HttpError`, `LoginRequired`; try-with-resources around every JDBC and file resource |
| Multithreading | `JudgeService` judges on an `ExecutorService`; the pool uses a `BlockingQueue`, `AtomicInteger` and a `ThreadLocal` transaction connection; `volatile` settings cache |
| Lambdas and streams | Route handlers as method references (`get("/admin", this::dashboard)`), row mappers, report aggregation with `Collectors.groupingBy` |
| File and process I/O | `java.nio.file` for the course files and sandbox directories, `ProcessBuilder` for running Python, GZIP + streaming JSON for the seed data |
| String handling and regex | Route patterns, slugs, username/email validation, the course file parser, output normalisation in `Judge` |
| Date and time API | `LocalDateTime` (UTC) for all timestamps, `DateTimeFormatter`, streak and timeline calculations |
| Security APIs | PBKDF2 password hashing (`javax.crypto`), `SecureRandom` tokens, constant-time comparison |

---

## 5. Database design and JDBC

### 5.1 Entity relationships
```
users ─┬─< problems >─┬─< test_cases
       │      │       └─< submissions >── users (student, grader)
       │      └─< challenge_problems >── challenges
       ├─< messages >── users (sender, recipient), submissions (optional attachment)
       ├─< lessons >── topics            lessons ─< quiz_questions
       └─< lesson_progress >── lessons
settings (key / value)
```

### 5.2 Tables
| Table | Purpose | Notable columns |
|---|---|---|
| `users` | Accounts | `username` and `email` unique, `password_hash`, `role`, `is_blocked` |
| `problems` | Exercises | `slug` unique, `difficulty`, limits, `status`, `review_note`, `created_by`, `reviewed_by` |
| `test_cases` | Sample and hidden tests | `input`, `expected_output` (MEDIUMTEXT), `is_sample` |
| `submissions` | Student code and its result | `verdict`, `passed`, `total`, `runtime_ms`, `grade`, `feedback`, `graded_by` |
| `messages` | Conversations | `sender_id`, `recipient_id`, `submission_id`, `read_at` |
| `challenges`, `challenge_problems` | Time-boxed exercise sets (many-to-many) | `start_at`, `end_at` |
| `topics`, `lessons`, `quiz_questions` | The course | `sort_order`, `source` (builtin / custom), `status` |
| `lesson_progress` | Per-student course progress | unique (`user_id`, `lesson_id`), best quiz score, `completed_at` |
| `settings` | System settings | `setting_key`, `setting_value` |

Foreign keys carry the clean-up rules: deleting a user cascades to their submissions, messages and progress
(`ON DELETE CASCADE`) and keeps content they wrote without an author (`ON DELETE SET NULL`).
The full DDL is in `src/main/resources/schema.sql`; `SchemaInitializer` runs it at start-up.

### 5.3 How JDBC is used
- **Connection pool** (`Database`): a bounded `BlockingQueue` of connections, validated on borrow, returned after each use.
- **Prepared statements everywhere**: `Database.query/update/insert/batch` bind every value as a parameter, so user
  input never becomes SQL text. Optional filters are built by `BaseDao.Where`, which still only appends `?` placeholders.
- **Row mapping**: each DAO has a `map(ResultSet)` method, passed as a `RowMapper<T>`; joins bring in the author,
  grader and exercise so pages do not issue one query per row.
- **Generated keys**: `Database.insert` returns the `AUTO_INCREMENT` id.
- **Transactions**: `Database.inTransaction` binds one connection to the thread, commits on success and rolls back on
  any exception. Used where two writes must agree: review + notification message, grade + message, challenge +
  exercise links, each seeded exercise + its test cases, the course sync.
- **Batch updates**: test cases, quiz questions and challenge links are inserted with `addBatch` / `executeBatch`.
- **Aggregation in SQL**: acceptance statistics use `COUNT`, `SUM(CASE ...)` and `COUNT(DISTINCT ...)` with `GROUP BY`;
  lists use `LIMIT ? OFFSET ?` pagination.

---

## 6. Servlets and web integration

| Topic | Implementation |
|---|---|
| Servlets | 13 `HttpServlet` subclasses registered with `@WebServlet`; `doGet` / `doPost` dispatch to route handlers |
| Filters | Four filters declared in `web.xml` in a fixed order: encoding, session, CSRF, role-based access |
| Listener | `AppListener` (`@WebListener`) starts the application on deploy and releases threads, connections and JDBC drivers on undeploy |
| Sessions | The session holds only the user id, the CSRF token and pending flash messages; the id is rotated on login; "remember me" extends it to 30 days |
| Request dispatching | Handlers forward to JSPs under `WEB-INF/views` (not reachable directly); successful POSTs redirect (Post/Redirect/Get) |
| JSP, EL, JSTL | 34 pages using `c:forEach`, `c:choose`, `c:out`, `fn:escapeXml`; no scriptlets |
| Custom tags | Tag files for the page layout, auth screens, badges, tables and pagination; a function library (`pyjudge.tld`) for dates, Markdown and JSON |
| JSON API | `ApiServlet` serves the editor: run, submit, poll, list |
| Error handling | `error-page` entries in `web.xml` route 400, 403, 404, 405 and 500 to one error page; API errors are JSON |
| Deployment descriptor | `web.xml` configures filters, session timeout and cookie flags, JSP prelude and error pages |

### 6.1 URL map
| Area | URLs |
|---|---|
| Public | `/`, `/problems`, `/problems/{slug}`, `/leaderboard`, `/u/{username}`, `/challenges`, `/challenges/{id}`, `/learn/...` |
| Auth | `/login`, `/register`, `/logout`, `/dashboard`, `/settings` |
| Student | `/student`, `/submissions`, `/submissions/{id}`, `/messages/...` |
| Instructor | `/instructor`, `/instructor/exercises`, `/instructor/lessons`, `/instructor/submissions`, `/instructor/submissions/{id}/grade`, `/instructor/reports`, `/studio/...`, `/challenges/new`, `/challenges/{id}/edit` |
| Admin | `/admin`, `/admin/users`, `/admin/users/{id}/edit`, `/admin/users/{id}/delete`, `/admin/content`, `/admin/approvals`, `/admin/exercises/{id}/review`, `/admin/lessons/{id}/review`, `/admin/settings` |
| API | `GET /api/problems`, `GET /api/problems/{slug}`, `POST /api/run`, `POST /api/submit`, `GET /api/submissions/{id}`, `GET /api/users/me/submissions`, `GET /api/leaderboard` |

---

## 7. Running untrusted code

| Measure | Process sandbox | Docker sandbox |
|---|---|---|
| Isolation | Separate `python -I` process, throwaway working directory, stripped environment | Fresh container per run, non-root user, read-only file system, all capabilities dropped |
| Time | Wall-clock timeout, then the process tree is killed; CPU `ulimit` | Wall-clock timeout, then `docker kill` |
| Memory | Address-space `ulimit` (Linux, macOS) | `--memory` hard limit |
| Network | Not restricted | `--network=none` |
| Output | Capped (64 KB for Run, 16 MB when judging) | Same |

The process sandbox is for development and classrooms on a trusted network. For anything public, set
`sandbox.backend=docker`. Syntax is checked first with Python's `compile()`, which never executes the code.

Web security: PBKDF2-hashed passwords, CSRF tokens on every POST, output escaping in every page, Markdown rendered
with raw HTML disabled, parameterised SQL, role checks in a filter and ownership checks in the handlers,
open-redirect protection on the login `next` parameter, HttpOnly session cookies.

---

## 8. Testing

| Suite | What it checks |
|---|---|
| `JudgeTest` | Verdicts for accepted, wrong answer, syntax error, runtime error, infinite loop, huge allocation; output truncation; Unicode; hidden test data never leaks |
| `WorkflowTest` | Each role's workflow through the services and a real database: user management, approval and rejection, grading and notification, messaging rules, challenges, settings, cascading deletes |
| `CourseTest` | Course files parse; every runnable example in every lesson actually runs; Markdown rendering and escaping; quiz grading and progress; lesson approval |
| `SeedTest` | The exercise bank is complete; each reference solution is accepted by the judge; seeding is repeatable |

`mvn test` runs them on an in-memory database. Setting `PYJUDGE_TEST_DB_URL` runs the same suites against MySQL.

---

## 9. Limitations and future work
- The process sandbox is not a security boundary; use the Docker backend when users are not trusted.
- Judging runs inside the web application. A separate worker service and a message queue would let it scale out.
- No schema migration tool: the schema is created if missing, but later column changes need a manual `ALTER TABLE`.
- Possible additions: more languages, live contests, plagiarism detection, email notifications.

## 10. References
- Jakarta Servlet 6.0 and Jakarta Server Pages 3.1 specifications
- JDBC 4.3 API documentation; MySQL 8 Reference Manual
- Python documentation: `compile()`, command-line option `-I`
- OWASP Cheat Sheets: Cross-Site Request Forgery Prevention, Password Storage, Cross Site Scripting Prevention
