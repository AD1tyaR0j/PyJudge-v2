<t:layout title="Practice Python in your browser">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/static/css/learn.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/static/js/home.js"></script></jsp:attribute>
  <jsp:body>
<section class="hero" id="hero">
  <canvas class="hero-canvas" aria-hidden="true"></canvas>
  <div class="snake-wrap">
    <button class="hero-snake" type="button" aria-label="Pet the Python snake">🐍</button>
    <div class="snake-bubble" role="status" aria-live="polite"></div>
  </div>
  <h1><span class="split-text">Write. Run.</span> <span class="grad">Get judged.</span></h1>
  <p>Solve Python problems right in your browser. There's nothing to install, and your code is checked automatically against hidden test cases.</p>
  <div class="typing" aria-hidden="true" data-typing='[">>> print(\"Hello, PyJudge!\")", ">>> sum(map(int, input().split()))", ">>> verdict = \"Accepted ✔\"", ">>> [x * x for x in range(10)]"]'></div>
  <div class="row" style="justify-content:center">
    <a class="btn btn-primary btn-lg magnetic" href="${ctx}/learn">${course.done > 0 ? 'Continue learning' : 'Start learning'}</a>
    <a class="btn btn-lg magnetic" href="${ctx}/problems">Browse exercises</a>
  </div>
  <p class="hero-hint" aria-hidden="true">✨ Move your mouse around, or click anywhere up here</p>
</section>

<div class="grid grid-4" style="margin-bottom:28px">
  <div class="card stat tilt"><div class="num">${stats.lessons}</div><div class="label">Lessons</div></div>
  <div class="card stat tilt"><div class="num">${stats.problems}</div><div class="label">Exercises</div></div>
  <div class="card stat tilt"><div class="num">${stats.users}</div><div class="label">Students</div></div>
  <div class="card stat tilt"><div class="num">${stats.submissions}</div><div class="label">Submissions judged</div></div>
</div>

<c:if test="${not empty homeTopics}">
<div class="row spread"><h2 style="margin:0">📚 Learn Python, step by step</h2>
  <a class="small" href="${ctx}/learn">Full course roadmap →</a></div>
<div class="grid home-topics" style="margin:14px 0 30px">
  <c:forEach var="tp" items="${homeTopics}">
    <a class="card tilt topic-chip" href="${ctx}/learn/${tp.topic.slug}" style="color:inherit">
      <span class="topic-chip-icon"><c:out value="${tp.topic.icon}"/></span>
      <div><strong><c:out value="${tp.topic.title}"/></strong>
        <div class="muted small">${tp.total} lessons<c:if test="${tp.done > 0}"> · ${tp.percent}% done</c:if></div></div>
    </a>
  </c:forEach>
</div>
</c:if>

<div class="grid home-cols">
  <div>
    <h2>Start here</h2>
    <div class="grid">
      <c:forEach var="p" items="${featured}">
        <a class="card tilt" href="${ctx}/problems/${p.slug}" style="color:inherit">
          <div class="row spread"><strong><c:out value="${p.title}"/></strong><t:difficulty d="${p.difficulty}"/></div>
          <div style="margin-top:6px"><t:ptags problem="${p}"/></div>
        </a>
      </c:forEach>
      <c:if test="${empty featured}">
        <div class="card muted">No exercises yet. An instructor or admin can add the first one.</div>
      </c:if>
    </div>
  </div>
  <c:if test="${site.leaderboard_enabled}"><div>
    <h2>Top coders</h2>
    <div class="card">
      <c:forEach var="r" items="${leaders}" varStatus="row">
        <div class="row spread" style="padding:4px 0">
          <span>${row.count}. <a href="${ctx}/u/${fn:escapeXml(r.user.username)}"><c:out value="${r.user.username}"/></a></span>
          <span class="muted">${r.score} pts</span>
        </div>
      </c:forEach>
      <c:if test="${empty leaders}">
        <p class="muted" style="margin:0">Nobody has solved anything yet. Be the first!</p>
      </c:if>
    </div>
  </div></c:if>
</div>

<c:if test="${not empty activeChallenges}">
<h2 style="margin-top:36px">🔥 Active challenges</h2>
<div class="grid grid-2">
  <c:forEach var="c" items="${activeChallenges}">
    <a class="card" href="${ctx}/challenges/${c.id}" style="color:inherit">
      <div class="card-head"><h3><c:out value="${c.title}"/></h3><span class="badge v-ac">Live</span></div>
      <p class="muted small" style="margin:0">${fn:length(c.approvedProblems)} exercises · ends ${pj:dt(c.endAt)} UTC</p>
    </a>
  </c:forEach>
</div>
</c:if>

<h2 style="margin-top:36px">How it works</h2>
<div class="grid grid-4">
  <div class="card"><h3>1 · Learn</h3><p class="muted">Short lessons with runnable examples and quizzes, from your first <code>print()</code> to BFS.</p></div>
  <div class="card"><h3>2 · Practice</h3><p class="muted">Each lesson links to exercises on the same topic, from FizzBuzz to edit distance.</p></div>
  <div class="card"><h3>3 · Submit</h3><p class="muted">Your code runs in a sandbox against hidden tests and you get a verdict in seconds.</p></div>
  <div class="card"><h3>4 · Grow</h3><p class="muted">Track your progress, earn achievements, join challenges, and get feedback from instructors.</p></div>
</div>
  </jsp:body>
</t:layout>
