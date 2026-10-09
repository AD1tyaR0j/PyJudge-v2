-- PyJudge database schema (MySQL 8+).
-- The application runs this file on every start; each statement is safe to repeat.
-- To create it by hand:  mysql -u root -p pyjudge < src/main/resources/schema.sql
-- All DATETIME values are UTC.

CREATE TABLE IF NOT EXISTS users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL,
  name          VARCHAR(100) NOT NULL DEFAULT '',
  email         VARCHAR(100) NOT NULL,
  bio           TEXT,
  password_hash VARCHAR(255) NOT NULL,
  role          VARCHAR(12)  NOT NULL DEFAULT 'student',
  is_blocked    BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at    DATETIME     NOT NULL,
  last_seen_at  DATETIME     NULL,
  CONSTRAINT uq_users_username UNIQUE (username),
  CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- A coding exercise. Instructors submit them; admins approve or reject them.
CREATE TABLE IF NOT EXISTS problems (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  title        VARCHAR(150) NOT NULL,
  slug         VARCHAR(150) NOT NULL,
  description  MEDIUMTEXT   NOT NULL,
  difficulty   VARCHAR(10)  NOT NULL DEFAULT 'Easy',
  time_limit   INT          NOT NULL DEFAULT 2,
  memory_limit INT          NOT NULL DEFAULT 128,
  tags         VARCHAR(255) NOT NULL DEFAULT '',
  starter_code TEXT,
  status       VARCHAR(10)  NOT NULL DEFAULT 'pending',
  review_note  TEXT,
  reviewed_by  INT          NULL,
  reviewed_at  DATETIME     NULL,
  created_by   INT          NULL,
  created_at   DATETIME     NOT NULL,
  updated_at   DATETIME     NOT NULL,
  CONSTRAINT uq_problems_slug UNIQUE (slug),
  CONSTRAINT fk_problems_author FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
  CONSTRAINT fk_problems_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Large tests can be megabytes of input, hence MEDIUMTEXT.
CREATE TABLE IF NOT EXISTS test_cases (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  problem_id      INT        NOT NULL,
  input           MEDIUMTEXT NOT NULL,
  expected_output MEDIUMTEXT NOT NULL,
  is_sample       BOOLEAN    NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_test_cases_problem FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS submissions (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  user_id      INT         NOT NULL,
  problem_id   INT         NOT NULL,
  code         MEDIUMTEXT  NOT NULL,
  verdict      VARCHAR(10) NOT NULL DEFAULT 'PENDING',
  passed       INT         NOT NULL DEFAULT 0,
  total        INT         NOT NULL DEFAULT 0,
  runtime_ms   INT         NULL,
  memory_kb    INT         NULL,
  error        MEDIUMTEXT,
  submitted_at DATETIME    NOT NULL,
  grade        INT         NULL,
  feedback     TEXT,
  graded_by    INT         NULL,
  graded_at    DATETIME    NULL,
  CONSTRAINT fk_submissions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_submissions_problem FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE CASCADE,
  CONSTRAINT fk_submissions_grader FOREIGN KEY (graded_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS messages (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  sender_id     INT      NOT NULL,
  recipient_id  INT      NOT NULL,
  body          TEXT     NOT NULL,
  submission_id INT      NULL,
  created_at    DATETIME NOT NULL,
  read_at       DATETIME NULL,
  CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_messages_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_messages_submission FOREIGN KEY (submission_id) REFERENCES submissions (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- A time-boxed set of exercises. Completed when a student solves them all inside the window.
CREATE TABLE IF NOT EXISTS challenges (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(150) NOT NULL,
  description TEXT,
  start_at    DATETIME     NOT NULL,
  end_at      DATETIME     NOT NULL,
  created_by  INT          NULL,
  created_at  DATETIME     NOT NULL,
  CONSTRAINT fk_challenges_author FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS challenge_problems (
  challenge_id INT NOT NULL,
  problem_id   INT NOT NULL,
  PRIMARY KEY (challenge_id, problem_id),
  CONSTRAINT fk_cp_challenge FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE CASCADE,
  CONSTRAINT fk_cp_problem FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- A chapter of the Python course, e.g. "Strings" or "Functions".
CREATE TABLE IF NOT EXISTS topics (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  slug        VARCHAR(80)  NOT NULL,
  title       VARCHAR(120) NOT NULL,
  icon        VARCHAR(16)  NOT NULL DEFAULT '',
  level       VARCHAR(20)  NOT NULL DEFAULT 'Beginner',
  summary     VARCHAR(300) NOT NULL DEFAULT '',
  description TEXT,
  sort_order  INT          NOT NULL DEFAULT 0,
  CONSTRAINT uq_topics_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- A tutorial page. Built-in lessons come from resources/curriculum; instructors can add more.
CREATE TABLE IF NOT EXISTS lessons (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  topic_id      INT          NOT NULL,
  slug          VARCHAR(120) NOT NULL,
  title         VARCHAR(150) NOT NULL,
  content       MEDIUMTEXT   NOT NULL,
  minutes       INT          NOT NULL DEFAULT 5,
  practice_tags VARCHAR(255) NOT NULL DEFAULT '',
  sort_order    INT          NOT NULL DEFAULT 0,
  source        VARCHAR(10)  NOT NULL DEFAULT 'custom',
  status        VARCHAR(10)  NOT NULL DEFAULT 'pending',
  review_note   TEXT,
  reviewed_by   INT          NULL,
  reviewed_at   DATETIME     NULL,
  created_by    INT          NULL,
  created_at    DATETIME     NOT NULL,
  updated_at    DATETIME     NOT NULL,
  CONSTRAINT uq_lessons_topic_slug UNIQUE (topic_id, slug),
  CONSTRAINT fk_lessons_topic FOREIGN KEY (topic_id) REFERENCES topics (id) ON DELETE CASCADE,
  CONSTRAINT fk_lessons_author FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
  CONSTRAINT fk_lessons_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quiz_questions (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  lesson_id    INT  NOT NULL,
  prompt       TEXT NOT NULL,
  options_json TEXT NOT NULL,
  answer       INT  NOT NULL DEFAULT 0,
  explanation  TEXT,
  sort_order   INT  NOT NULL DEFAULT 0,
  CONSTRAINT fk_quiz_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lesson_progress (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  user_id      INT      NOT NULL,
  lesson_id    INT      NOT NULL,
  completed_at DATETIME NULL,
  quiz_score   INT      NULL,
  quiz_total   INT      NULL,
  attempts     INT      NOT NULL DEFAULT 0,
  updated_at   DATETIME NOT NULL,
  CONSTRAINT uq_progress_user_lesson UNIQUE (user_id, lesson_id),
  CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- System-wide settings edited by admins, stored as key/value rows.
CREATE TABLE IF NOT EXISTS settings (
  setting_key   VARCHAR(50) PRIMARY KEY,
  setting_value TEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
