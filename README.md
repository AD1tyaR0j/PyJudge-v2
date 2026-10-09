# 🐍 PyJudge (v2): Python-Based Online Coding & Learning Platform

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Jakarta Servlet](https://img.shields.io/badge/Jakarta_Servlet-6.0-blue.svg)](https://jakarta.ee/specifications/servlet/6.0/)
[![JSP & JSTL](https://img.shields.io/badge/JSP_3.1-JSTL_3.0-blueviolet.svg)](https://jakarta.ee/specifications/pages/3.1/)
[![Tomcat 10.1](https://img.shields.io/badge/Tomcat-10.1-yellowgreen.svg)](https://tomcat.apache.org/)
[![Python 3](https://img.shields.io/badge/Python-3.10+-yellow.svg)](https://www.python.org/)
[![Build & Test](https://img.shields.io/badge/Build-179%20Passing-brightgreen.svg)]()
[![Pure Java](https://img.shields.io/badge/Frameworks-Zero%20(Pure%20Java%20%2B%20JDBC)-red.svg)]()

> A full-scale, production-ready online coding, judging, and educational platform for learning Python.
> Built entirely in **Pure Core Java, JDBC, Jakarta Servlets 6.0, and JSP 3.1** (strictly no Spring, no Hibernate, and no ORM frameworks).
>
> Designed, engineered, and verified specifically against **Topic 36: Python-Based Online Coding Platform** and the **Java Web-Based Projects Marking Rubric**.

---

## 📑 Detailed Table of Contents
1. [What is the Use of This? (Problem Statement & Purpose)](#1-what-is-the-use-of-this-problem-statement--purpose)
2. [What is New in This (v2 Innovations)?](#2-what-is-new-in-this-v2-innovations)
3. [How We Built It According to the Problem Statement (Topic 36 Specification)](#3-how-we-built-it-according-to-the-problem-statement-topic-36-specification)
4. [How We Implemented the Marking Rubrics (Deep-Dive Mapping)](#4-how-we-implemented-the-marking-rubrics-deep-dive-mapping)
   - [Rubric 1: Problem Understanding & Solution Design (8 Marks)](#rubric-1-problem-understanding--solution-design-8-marks)
   - [Rubric 2: Core Java Concepts (10 Marks)](#rubric-2-core-java-concepts-10-marks)
   - [Rubric 3: Database Integration & JDBC (8 Marks)](#rubric-3-database-integration--jdbc-8-marks)
   - [Rubric 4: Servlets & Web Integration (7 Marks)](#rubric-4-servlets--web-integration-7-marks)
5. [Complete Feature Catalog](#5-complete-feature-catalog)
   - [Administrator Capabilities](#administrator-features)
   - [Instructor & Authoring Studio Capabilities](#instructor-features)
   - [Student Learning & Problem-Solving Capabilities](#student-features)
   - [Automated Judge & Sandbox Subsystem](#automated-judge--sandbox-subsystem)
6. [How to Use This Platform (End-to-End User Manual)](#6-how-to-use-this-platform-end-to-end-user-manual)
   - [Starting the Application](#step-1-starting-the-application)
   - [Student User Journey](#step-2-student-user-journey-walkthrough)
   - [Instructor User Journey](#step-3-instructor-user-journey-walkthrough)
   - [Administrator User Journey](#step-4-administrator-user-journey-walkthrough)
7. [System Architecture & Security Design](#7-system-architecture--security-design)
8. [Setup, Installation & Running Options](#8-setup-installation--running-options)
   - [Option A: Zero-Setup Embedded Mode (H2 Database)](#option-a-zero-setup-embedded-mode-h2-database-recommended)
   - [Option B: Production Mode (MySQL 8)](#option-b-production-mode-mysql-8)
   - [Pre-Seeded Demo Accounts](#pre-seeded-demo-accounts)
9. [Automated Testing & Quality Verification](#9-automated-testing--quality-verification)
10. [Command-Line Tools (Admin CLI)](#10-command-line-tools-admin-cli)

---

## 1. What is the Use of This? (Problem Statement & Purpose)

### The Real-World Problem
- **Setup Friction for Beginners**: Novice programmers frequently lose hours configuring Python installations, managing OS environment paths, and wrestling with dependencies before writing their first line of code.
- **Grading Overload for Instructors**: Evaluating programming assignments manually is slow, error-prone, subjective, and completely unscalable for university cohorts or coding bootcamps.
- **Fragmented Learning Experiences**: Students usually jump between tutorials on one website, coding sandboxes on another, and messaging apps for instructor help, losing continuity and track of their growth.
- **Quality Control Deficit**: Open platforms often suffer from unverified or broken coding problems with incorrect test cases when content is submitted without moderation.

### The PyJudge Solution
**PyJudge** provides a self-contained, enterprise-grade educational ecosystem where:
- **Students** code entirely within modern browser-based editors, execute code with real-time custom inputs, get judged against hidden test cases in seconds, earn achievements, and progress through a 32-lesson curriculum.
- **Instructors** author problems with rich Markdown descriptions and test suites, build structured tutorial lessons with quizzes, grade submissions with qualitative criteria, and mentor students through integrated messaging.
- **Administrators** maintain academic integrity and system stability through a gated approval workflow, user management controls, and system-wide limit throttles.

---

## 2. What is New in This (v2 Innovations)?

Compared to conventional online judges or rudimentary servlet demos, PyJudge v2 introduces several architectural and UX advancements:

| Innovation | What Was Typical Before | What PyJudge v2 Delivers |
|---|---|---|
| **Zero-Setup Portability** | Required setting up and running MySQL 8 services locally before testing. | Built-in **embedded H2 database engine in MySQL compatibility mode**. The platform boots instantly anywhere in 1 command with zero external database configuration. |
| **Dual Execution Sandboxes** | Inflexible execution, often un-sandboxed or tightly bound to Docker. | Pluggable `Sandbox` interface supporting **`ProcessSandbox`** (low-overhead local development with process isolation, output truncation, and execution limits) and **`DockerSandbox`** (production-grade containerized isolation). |
| **32-Lesson Integrated Curriculum** | Static exercise banks without guided instructional content. | Full **11-topic, 32-lesson Python course** built directly into the app (`/learn`) complete with in-browser runnable examples, code editors, and automated quizzes. |
| **Content Approval Lifecycle** | Anyone publishes exercises, causing clutter and broken tests. | Enterprise **editorial workflow**: Instructor problems start in `PENDING` status. Administrators must inspect, preview, and approve or reject them with mandatory reviewer notes. |
| **Hybrid Dual-Grading System** | Either purely automated (AC/WA) or purely manual. | **Dual evaluation pipeline**: Instant automated test-case evaluation (AC, WA, TLE, MLE, RE, CE) combined with an instructor qualitative grading queue (0–100 score + structured feedback) with a *"Save & Grade Next"* flow. |
| **Cross-Platform Output Normalization** | CRLF line-ending discrepancies causing false `Wrong Answer` failures on Windows. | Native newline and whitespace normalizer in the judging engine ensuring reliable, identical test results across Windows, macOS, and Linux. |
| **Glassmorphism & Cyberpunk UI** | Monolithic, outdated table-based JSP interfaces. | Responsive glassmorphism interface featuring dynamic canvas particle meshes, interactive mascot animations, dark-mode themes, and motion accessibility toggles. |

---

## 3. How We Built It According to the Problem Statement (Topic 36 Specification)

The project directly targets **Topic 36: Python-Based Online Coding Platform** from the 3rd Semester SCSE curriculum. Every requirement specified in the brief has a dedicated, verified implementation:

```
Summary from Brief:
"The goal is to develop an online coding platform for learning Python, providing interactive coding
exercises and challenges. Administrators will manage users and content."
```

### Direct Feature-to-Code Mapping Table

| User Role | Specified Requirement (from Brief) | Specified Inputs & Outputs | Concrete Implementation in PyJudge | Source Files in Codebase |
|---|---|---|---|---|
| **Admin** | **1. User Management** | **Input**: User details (name, email, role)<br>**Output**: Confirmation message for successful user creation / update / deletion<br>**Functionality**: Manage user accounts and roles | Full user administration table with search, role filtering, account creation modal, inline role promotion/demotion, account blocking/unblocking, and deletion with cascading cleanup. | [`AdminServlet.java`](src/main/java/com/pyjudge/web/servlet/AdminServlet.java)<br>[`AccountService.java`](src/main/java/com/pyjudge/service/AccountService.java)<br>[`UserDao.java`](src/main/java/com/pyjudge/dao/UserDao.java)<br>[`users.jsp`](src/main/webapp/WEB-INF/views/admin/users.jsp) |
| **Admin** | **2. Exercise Content Management** | **Input**: Exercise details (title, description, difficulty)<br>**Output**: Content approval status<br>**Functionality**: Approve or reject coding exercises submitted by instructors | Dedicated moderation queue showing submitted instructor problems, previewing Markdown statements and test counts. Admins approve or reject with a required feedback note. | [`AdminServlet.java`](src/main/java/com/pyjudge/web/servlet/AdminServlet.java)<br>[`ReviewService.java`](src/main/java/com/pyjudge/service/ReviewService.java)<br>[`ProblemDao.java`](src/main/java/com/pyjudge/dao/ProblemDao.java)<br>[`approvals.jsp`](src/main/webapp/WEB-INF/views/admin/approvals.jsp) |
| **Admin** | **3. System Settings** | **Input**: Configuration settings<br>**Output**: Confirmation message for successful settings update<br>**Functionality**: Manage system-wide settings | Dynamic configuration panel for site title, announcement banners, open registration toggle, approval gating toggle, messaging permissions, and default execution timeouts. | [`SettingsService.java`](src/main/java/com/pyjudge/service/SettingsService.java)<br>[`SettingDao.java`](src/main/java/com/pyjudge/dao/SettingDao.java)<br>[`settings.jsp`](src/main/webapp/WEB-INF/views/admin/settings.jsp) |
| **Admin** | **Admin Dashboard** | **Input**: None<br>**Output**: Analytics, counts, recent submissions | Live KPI cards (users, problems, submissions), submissions-per-day charts, active student metrics, and pending approval alerts. | [`AdminServlet.java`](src/main/java/com/pyjudge/web/servlet/AdminServlet.java)<br>[`StatsService.java`](src/main/java/com/pyjudge/service/StatsService.java)<br>[`dashboard.jsp`](src/main/webapp/WEB-INF/views/admin/dashboard.jsp) |
| **Instructor** | **1. Create Coding Exercises** | **Input**: Exercise details (title, description, difficulty)<br>**Output**: Confirmation message for successful creation<br>**Functionality**: Create and manage coding exercises | Authoring Studio supporting Markdown problem descriptions, difficulty assignment (Easy, Medium, Hard), execution time & memory limits, starter code templates, sample tests, and hidden tests. | [`StudioServlet.java`](src/main/java/com/pyjudge/web/servlet/StudioServlet.java)<br>[`StudioService.java`](src/main/java/com/pyjudge/service/StudioService.java)<br>[`exercise_form.jsp`](src/main/webapp/WEB-INF/views/studio/exercise_form.jsp) |
| **Instructor** | **2. Review Student Submissions** | **Input**: Submission details<br>**Output**: Feedback and grading<br>**Functionality**: Review and grade student submissions | Review queue for submitted solutions. Instructors inspect code, view automated judge verdicts, assign a 0–100 rubric grade, and attach detailed qualitative comments. | [`InstructorServlet.java`](src/main/java/com/pyjudge/web/servlet/InstructorServlet.java)<br>[`ReviewService.java`](src/main/java/com/pyjudge/service/ReviewService.java)<br>[`submissions.jsp`](src/main/webapp/WEB-INF/views/instructor/submissions.jsp) |
| **Instructor** | **3. Interact with Students** | **Input**: Interaction details (messages, feedback)<br>**Output**: Confirmation message<br>**Functionality**: Communicate with students | Threaded messaging inbox. Instructors can message any student directly. Grading a submission automatically sends an linked feedback notification. | [`MessagesServlet.java`](src/main/java/com/pyjudge/web/servlet/MessagesServlet.java)<br>[`MessageService.java`](src/main/java/com/pyjudge/service/MessageService.java)<br>[`thread.jsp`](src/main/webapp/WEB-INF/views/messages/thread.jsp) |
| **Student** | **1. Complete Coding Exercises** | **Input**: Exercise selection<br>**Output**: Exercise results and feedback<br>**Functionality**: Complete interactive coding exercises | Interactive web code editor. Features **Run** (custom stdin execution) and **Submit** (background evaluation across test cases with animated results). | [`ProblemServlet.java`](src/main/java/com/pyjudge/web/servlet/ProblemServlet.java)<br>[`ApiServlet.java`](src/main/java/com/pyjudge/web/servlet/ApiServlet.java)<br>[`JudgeService.java`](src/main/java/com/pyjudge/judge/JudgeService.java)<br>[`problem.jsp`](src/main/webapp/WEB-INF/views/problem.jsp) |
| **Student** | **2. Track Progress** | **Input**: Progress details<br>**Output**: Progress report<br>**Functionality**: Track and view learning progress | Student dashboard with difficulty breakdown charts (Easy/Medium/Hard), solved-over-time trends, active day streak counter, earned achievements, and average grade. | [`AccountServlet.java`](src/main/java/com/pyjudge/web/servlet/AccountServlet.java)<br>[`StatsService.java`](src/main/java/com/pyjudge/service/StatsService.java)<br>[`dashboard.jsp`](src/main/webapp/WEB-INF/views/student/dashboard.jsp) |
| **Student** | **3. Interact with Instructors** | **Input**: Interaction details (messages, feedback)<br>**Output**: Confirmation message<br>**Functionality**: Communicate with instructors | Students can start conversations with instructors from `/messages/new` or click *"Ask an instructor about this"* directly from any submission report. | [`MessagesServlet.java`](src/main/java/com/pyjudge/web/servlet/MessagesServlet.java)<br>[`compose.jsp`](src/main/webapp/WEB-INF/views/messages/compose.jsp) |

---

## 4. How We Implemented the Marking Rubrics (Deep-Dive Mapping)

The project was structured from the ground up to achieve maximum scores across the **33-mark Java Web-Based Projects Marking Rubric**:

```
Marking Rubric Breakdown:
1. Problem Understanding & Solution Design: 8 Marks
2. Core Java Concepts:                     10 Marks
3. Database Integration (JDBC):            8 Marks
4. Servlets & Web Integration:             7 Marks
──────────────────────────────────────────────────
Total Score:                              33 Marks
```

### Rubric 1: Problem Understanding & Solution Design (8 Marks)
- **Layered 4-Tier Architecture**: Built with distinct layers — Presentation (JSP/JSTL), Web (Servlets/Filters), Business Logic (Services), and Data Access (DAOs) — with zero leaking of SQL or presentation logic across boundaries.
- **Asynchronous Execution Model**: Student code execution can take seconds per test case. Instead of blocking the servlet HTTP request thread, `ApiServlet` hands the submission to `JudgeService` (which uses a bounded thread pool), immediately responds with `PENDING`, and lets the browser poll for completion.
- **Pluggable Sandbox Strategy**: The system relies on the `Sandbox` interface. Developers can run `ProcessSandbox` locally without installing Docker, while production deployments can swap to `DockerSandbox` via a single configuration key (`sandbox.backend=docker`).

### Rubric 2: Core Java Concepts (10 Marks)
| Core Java Concept | Concrete Implementation & Location in Codebase |
|---|---|
| **Encapsulation & Domain Logic** | Domain models (`User`, `Problem`, `Submission`, `TestCase`, `Challenge`) have private fields with getters/setters. Domain business rules live with the entity (e.g. `Problem.editableBy(user)` in [`Problem.java`](src/main/java/com/pyjudge/model/Problem.java)). |
| **Inheritance & Abstract Classes** | - `BaseServlet` encapsulates routing and request dispatching for 13 servlets.<br>- `BaseDao` standardizes parameter mapping and query building for 11 DAOs.<br>- `AbstractSandbox` implements the Template Method pattern: managing temp folders, timeouts, and stream parsing, while subclasses specify execution commands. |
| **Interfaces & Polymorphism** | - `Sandbox` interface decouples judging from OS execution mechanisms.<br>- `RowMapper<T>` functional interface maps `ResultSet` rows to typed objects.<br>- `Form` interface unifies form extraction, validation, and error reporting. |
| **Modern Java Records** | Immutable data carriers implemented as records: `RunResult`, `JudgeResult`, `Outcome`, `SubmissionDao.Solve`, `CurriculumParser.LessonSpec`. |
| **Generics** | - Generic pagination container: `Page<T>` in [`Page.java`](src/main/java/com/pyjudge/dao/Page.java).<br>- Generic database query helper: `Database.query(String, RowMapper<T>, Object...)`.<br>- Generic transaction wrapper: `Database.inTransaction(Supplier<T>)`. |
| **Collections Framework** | - `ArrayBlockingQueue` in [`Database.java`](src/main/java/com/pyjudge/db/Database.java) for the thread-safe connection pool.<br>- `TreeSet` for alphabetically sorted exercise tags.<br>- `LinkedHashMap` for ordered rendering in view contexts. |
| **Multithreading & Synchronization** | - `JudgeService` utilizes an `ExecutorService` thread pool for judging submissions in the background.<br>- `AtomicInteger` tracks active connections in the connection pool.<br>- `ThreadLocal<Connection>` ensures transactional isolation per thread during multi-step database writes. |
| **Robust Exception Handling** | Custom exception hierarchy: `DaoException`, `SandboxException`, `CurriculumException`, `HttpError`, `LoginRequired`. Try-with-resources is utilized across all JDBC statements and file streams. |
| **Java NIO & Process I/O** | `java.nio.file.Files` for directory sandboxing and lesson parsing; `ProcessBuilder` with process-tree lifecycle management (`ProcessHandle::destroyForcibly`). |
| **Security & Cryptography** | `javax.crypto.SecretKeyFactory` with `PBKDF2WithHmacSHA256` (310,000 iterations, 16-byte random salt) for password hashing; `SecureRandom` for anti-CSRF token generation. |

### Rubric 3: Database Integration & JDBC (8 Marks)
- **100% Pure JDBC**: No Hibernate, no Spring Data, and no third-party ORMs. Every query is explicit and visible.
- **Strict Parameterized Statements**: 100% of queries use `PreparedStatement` with `?` placeholders. Dynamic query filtering in `BaseDao.Where` exclusively uses parameterized conditions.
- **Custom Connection Pool**: Built in [`Database.java`](src/main/java/com/pyjudge/db/Database.java) with a bounded `ArrayBlockingQueue<Connection>`, connection usability verification, and graceful release.
- **ACID Transaction Management**: Multi-table updates execute inside `Database.inTransaction(Supplier<T>)`:
  - Submitting a review note + updating exercise status.
  - Grading a submission + sending a notification message to the student.
  - Creating a challenge + linking multiple problem IDs.
  - Seeding an exercise + batch-inserting its test cases.
- **Batch Processing**: Test cases, curriculum quiz questions, and challenge relationships are inserted using `PreparedStatement.addBatch()` and `executeBatch()`.
- **12 Relational Tables**: Full schema with primary keys, unique constraints, foreign keys, and cascading delete rules (`ON DELETE CASCADE` / `ON DELETE SET NULL`) defined in [`schema.sql`](src/main/resources/schema.sql).

### Rubric 4: Servlets & Web Integration (7 Marks)
- **13 Jakarta Servlets**: Mapped via `@WebServlet` and extending `BaseServlet` to handle routing (`/problems`, `/student`, `/instructor`, `/admin`, `/studio`, `/learn`, `/challenges`, `/messages`, etc.).
- **Four Ordered Servlet Filters**:
  1. `EncodingFilter`: Forces UTF-8 request and response encoding.
  2. `SessionFilter`: Rotates sessions securely, retrieves current user, and injects notifications into the request scope.
  3. `CsrfFilter`: Rejects any state-altering HTTP request (POST/PUT/DELETE) lacking a valid session token.
  4. `AccessFilter`: Guards restricted paths (`/admin/*`, `/instructor/*`, `/studio/*`) based on user role.
- **Lifecycle Listener**: `@WebListener` [`AppListener.java`](src/main/java/com/pyjudge/web/AppListener.java) wires services on startup and gracefully stops background threads and connection pools on container shutdown.
- **Zero Scriptlet JSP Views**: 34 JSP pages cleanly written using JSTL 3.0 (`c:forEach`, `c:if`, `c:choose`, `c:out`), custom tag files (`/WEB-INF/tags/`), and custom EL functions (`pyjudge.tld`).

---

## 5. Complete Feature Catalog

### Administrator Features
- **Dashboard (`/admin`)**: Real-time stats, submissions-per-day trend graphs, active students count, and pending approval queues.
- **User Management (`/admin/users`)**: Search users by name/email, filter by role, edit profile details, block/unblock accounts, and delete users.
- **Content Moderation (`/admin/approvals`)**: Preview instructor-submitted exercises and curriculum lessons; approve for publishing or reject with required feedback.
- **Global Content Inventory (`/admin/content`)**: Global catalog of all exercises across the platform with status filtering and edit overrides.
- **System Settings (`/admin/settings`)**: Live modification of site name, banner notices, open registration, approval requirements, messaging rules, and execution timeouts.

### Instructor Features
- **Exercise Studio (`/studio/exercises/new`)**: Markdown-enabled problem editor, tag selector, starter code generator, and test-case builder (sample + hidden).
- **Curriculum Studio (`/instructor/lessons`)**: Create custom lessons with markdown explanations, runnable Python code examples, and interactive comprehension quizzes.
- **Submissions Grading Queue (`/instructor/submissions`)**: Filter submissions by exercise or grading status; view code diffs and judge outputs; assign 0–100 marks and text feedback.
- **Threaded Student Messaging (`/messages`)**: Direct messaging inbox to converse with students; automated notification on graded assignments.
- **Performance Analytics (`/instructor/reports`)**: Per-problem acceptance rate charts, difficulty curves, and per-student completion trackers.

### Student Features
- **In-Browser Code Editor (`/problems/{slug}`)**:
  - Syntax-highlighted code editor with keyboard shortcuts.
  - **Run**: Execute code against custom inputs with instant stdout/stderr display.
  - **Submit**: Queue code for evaluation against all test cases with animated progress feedback.
- **Python Course (`/learn`)**:
  - 11 core topics covering 32 structured lessons from basic variables to graph algorithms.
  - Interactive runnable code demos embedded directly inside lessons.
  - Multiple-choice checkpoint quizzes with instant score tracking.
- **Time-Boxed Challenges (`/challenges`)**: Join competitive coding sprints with start/end windows and live challenge standings.
- **Gamified Student Dashboard (`/student`)**: Track solved problems by difficulty, maintain daily login/coding streaks, accumulate points, and unlock achievements.
- **Global Leaderboard (`/leaderboard`)**: Dynamic leaderboard ranking students by problem difficulty points and challenge results.

### Automated Judge & Sandbox Subsystem
- **Instant Verdicts**:
  - `AC` (Accepted): Passed all sample and hidden test cases.
  - `WA` (Wrong Answer): Output differed from expected output.
  - `TLE` (Time Limit Exceeded): Code execution timed out (e.g. infinite loops).
  - `MLE` (Memory Limit Exceeded): Memory usage exceeded allocated limits.
  - `RE` (Runtime Error): Unhandled Python exceptions or non-zero exit codes.
  - `CE` (Compilation / Syntax Error): Code failed syntax pre-check (`compile()`).
- **Whitespace & CRLF Normalization**: Output is normalized against trailing spaces and OS line endings to prevent false negatives.
- **Output Protection**: Maximum output caps prevent buffer overruns or memory denial-of-service from malicious `print()` loops.

---

## 6. How to Use This Platform (End-to-End User Manual)

### Step 1: Starting the Application
Make sure Java 17+, Maven, and Python 3 are in your PATH, then run:
```bash
mvn package cargo:run
```
Open **[http://localhost:8080/pyjudge](http://localhost:8080/pyjudge)** in your web browser.

---

### Step 2: Student User Journey (Walkthrough)
1. **Sign Up / Log In**:
   - Click **Sign up** on the navigation bar, enter a username, email, and password.
   - Alternatively, log in using the demo account: `student` / `student12345`.
2. **Learn Python**:
   - Go to **Learn** (`/pyjudge/learn`).
   - Click on **Getting Started → Hello, Python!**.
   - Read the lesson, run the interactive code snippet in the browser, and answer the checkpoint quiz at the bottom to record your progress.
3. **Solve Practice Exercises**:
   - Go to **Exercises** (`/pyjudge/problems`).
   - Select a problem (e.g. *Sum of Two Numbers* or *Reverse a String*).
   - Write your Python solution in the code editor.
   - Click **Run** to test with your own input.
   - Click **Submit** to trigger the automated judge. Watch the real-time verdict animation change to **Accepted ✔**.
4. **Join a Challenge**:
   - Go to **Challenges** (`/pyjudge/challenges`), open the active *Python Starter Sprint*, and solve the problems before time runs out.
5. **Ask an Instructor**:
   - Open any of your past submissions from the dashboard or problem page and click **"Ask an instructor about this"** to open a direct discussion thread with an instructor.

---

### Step 3: Instructor User Journey (Walkthrough)
1. **Log In**:
   - Log in with: `instructor` / `instructor12345`.
2. **Create a New Coding Exercise**:
   - Open **Exercises** under the instructor navigation and click **"New exercise"** (or visit `/pyjudge/studio/exercises/new`).
   - Fill in the exercise title, slug, difficulty (Easy/Medium/Hard), and problem statement in Markdown.
   - Provide starter code for students.
   - Add at least one **Sample Test Case** (visible to students) and **Hidden Test Cases** (used for grading).
   - Click **Save exercise**. Your exercise is saved in `PENDING` status awaiting admin review.
3. **Grade Student Submissions**:
   - Click **Submissions** (`/pyjudge/instructor/submissions`) to view the queue of student submissions.
   - Click **Grade** on any submission.
   - Review the student's code, runtime, and judge verdict.
   - Enter a score between 0 and 100, add personalized feedback notes, and click **"Save & Grade Next"**.
4. **View Class Reports**:
   - Navigate to **Reports** (`/pyjudge/instructor/reports`) to inspect acceptance-rate charts, hard exercises, and student completion counts.

---

### Step 4: Administrator User Journey (Walkthrough)
1. **Log In**:
   - Log in with: `admin` / `admin12345`.
2. **Moderate Instructor Submissions**:
   - Navigate to **Approvals** (`/pyjudge/admin/approvals`).
   - You will see exercises or lessons submitted by instructors.
   - Click **Review**, inspect the statement and test cases, then click **Approve** (to publish immediately) or **Reject** (with a reason sent back to the instructor).
3. **Manage Users & Roles**:
   - Go to **Users** (`/pyjudge/admin/users`).
   - Search for any student, click **Edit**, and change their role to `INSTRUCTOR` or `ADMIN`.
   - You can also block abusive accounts or delete users.
4. **Configure System Settings**:
   - Navigate to **Settings** (`/pyjudge/admin/settings`).
   - Change the platform site name, toggle whether instructor exercises require approval, or set an announcement banner visible to all users.

---

## 7. System Architecture & Security Design

```
  Browser (JSP Views, editor.js, learn.js)
       │ HTTP / JSON API
       ▼
 ┌────────────────────────── Tomcat 10.1 (Servlet Container) ──────────────────────────┐
 │  Filters:   EncodingFilter ──► SessionFilter ──► CsrfFilter ──► AccessFilter         │
 │  Servlets:  Auth, Account, Admin, Instructor, Studio, Problem, Learn, etc.          │
 │  Views:     34 JSP Pages (JSTL 3.0 + Custom Tag Library)                            │
 ├─────────────────────────────────────────────────────────────────────────────────────┤
 │  Services:  AccountService, StudioService, ReviewService, CourseService,            │
 │             StatsService, MessageService, ChallengeService                          │
 ├────────────────────────────────────────┬────────────────────────────────────────────┤
 │  DAOs:      11 JDBC DAOs               │  Judge:  JudgeService (ExecutorService)    │
 │  Database:  Custom Connection Pool,    │          Judge Engine                      │
 │             Transactions, RowMappers   │          ├ ProcessSandbox (python3 -I)     │
 │                                        │          └ DockerSandbox  (Containerized)  │
 └───────────────────┬────────────────────┴────────────────────────────────────────────┘
                     ▼
           H2 Embedded / MySQL 8
```

### Security Defenses
- **Process Sandboxing**: `python -I` isolated mode disables environment variables, site-packages, and user directories.
- **Password Security**: Salted PBKDF2 (`PBKDF2WithHmacSHA256`) with 310,000 hashing rounds.
- **CSRF Tokens**: Verified on every state-altering HTTP request.
- **SQL Injection Prevention**: 100% parameterized statements across all queries.
- **XSS Prevention**: HTML-escaped output rendering and raw HTML disabled in Markdown parsing.
- **Role-Based Access Control**: Filter-level enforcement preventing privilege escalation.

---

## 8. Setup, Installation & Running Options

### System Prerequisites
| Dependency | Recommended Version |
|---|---|
| **Java JDK** | 17 LTS+ |
| **Apache Maven** | 3.8+ |
| **Python** | 3.8+ (`python` / `python3`) |

---

### Option A: Zero-Setup Embedded Mode (H2 Database) — *Recommended*
PyJudge is pre-configured with embedded H2 support in MySQL compatibility mode. **No external database installation is required.**

```bash
# Clone the repository
git clone git@github.com:AD1tyaR0j/PyJudge-v2.git
cd PyJudge-v2

# Build the WAR and start Tomcat
mvn package cargo:run
```
Visit **[http://localhost:8080/pyjudge](http://localhost:8080/pyjudge)**!

---

### Option B: Production Mode (MySQL 8)
To connect PyJudge to a production MySQL instance:

1. Create a MySQL database:
   ```sql
   CREATE DATABASE pyjudge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Edit `src/main/resources/application.properties`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/pyjudge?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=UTC&allowPublicKeyRetrieval=true&useSSL=false
   db.user=your_username
   db.password=your_password
   ```
3. Run the application:
   ```bash
   mvn package cargo:run
   ```

---

### Pre-Seeded Demo Accounts

On the first start, PyJudge automatically seeds the database with 37 coding problems, 32 course lessons, and 3 starter accounts:

| Role | Username | Password | Purpose |
|---|---|---|---|
| **Admin** | `admin` | `admin12345` | System configuration, content approval, user management |
| **Instructor** | `instructor` | `instructor12345` | Problem creation studio, grading queue, class reports |
| **Student** | `student` | `student12345` | Complete exercises, participate in challenges, course lessons |

---

## 9. Automated Testing & Quality Verification

PyJudge comes with comprehensive automated test suites:

```bash
mvn test
```

```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.pyjudge.CourseTest
Tests run: 143, Failures: 0, Errors: 0, Skipped: 0  (All 32 lesson Markdown files & code examples pass)
Running com.pyjudge.JudgeTest
Tests run: 15, Failures: 0, Errors: 0, Skipped: 1   (Syntax, runtime, timeouts, and unicode tests pass)
Running com.pyjudge.SeedTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0    (All 37 problem solutions verified Accepted)
Running com.pyjudge.WorkflowTest
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0   (Admin, Instructor, and Student workflows pass)

Results:
Tests run: 179, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
```

---

## 10. Command-Line Tools (Admin CLI)

PyJudge includes a command-line interface for head-less administration:

```bash
# Compile project
mvn compile

# Change user role: student | instructor | admin
mvn -q compile exec:java -Dexec.args="set-role alice instructor"

# Reset a user's password
mvn -q compile exec:java -Dexec.args="set-password alice newpass123"

# Re-run starter data seeding
mvn -q compile exec:java -Dexec.args="seed"
```

---

## 📄 License
This project is open-source educational software distributed under the MIT License.
