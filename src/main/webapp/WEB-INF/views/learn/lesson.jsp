<t:layout title="${lesson.title} · Learn">
  <jsp:attribute name="head">
<link rel="stylesheet" href="${ctx}/static/css/learn.css">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/atom-one-dark.min.css">
  </jsp:attribute>
  <jsp:attribute name="scripts">
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
<script>window.LESSON = ${pj:json(lessonJs)};</script>
<script src="${ctx}/static/js/learn.js"></script>
  </jsp:attribute>
  <jsp:body>
<c:set var="completed" value="${progress != null and progress.completed}"/>
<div class="read-progress" aria-hidden="true"><span></span></div>
<div class="lesson-layout">
  <aside class="lesson-side">
    <a class="small" href="${ctx}/learn/${topic.slug}">← <c:out value="${topic.icon}"/> <c:out value="${topic.title}"/></a>
    <ol class="side-list">
      <c:forEach var="l" items="${siblings}" varStatus="row">
        <c:set var="lp" value="${course.progress[l.id]}"/>
        <c:set var="ldone" value="${lp != null and lp.completed}"/>
        <li class="${l.id == lesson.id ? 'current' : ''} ${ldone ? 'done' : ''}">
          <a href="${ctx}/learn/${topic.slug}/${l.slug}">
            <span class="side-mark">${ldone ? '✓' : row.count}</span><c:out value="${l.title}"/></a>
        </li>
      </c:forEach>
    </ol>
    <a class="small muted" href="${ctx}/learn">All topics</a>
  </aside>

  <article class="lesson-main">
    <header class="lesson-header">
      <div class="muted small"><c:out value="${topic.icon}"/> <c:out value="${topic.title}"/> · ${lesson.minutes} min read
        <c:if test="${not empty lesson.author and not lesson.builtin}"> · by <c:out value="${lesson.author.displayName}"/></c:if></div>
      <h1><c:out value="${lesson.title}"/></h1>
      <div class="row">
        <c:if test="${completed}"><span class="badge v-ac">✓ Completed</span></c:if>
        <c:if test="${not lesson.approved}"><t:status s="${lesson.status}"/><span class="small muted">Preview: students can't see this yet.</span></c:if>
        <c:if test="${lesson.editableBy(currentUser)}"><a class="small" href="${ctx}/studio/lessons/${lesson.id}/edit">Edit lesson</a></c:if>
      </div>
    </header>

    <%-- Rendered by MarkdownService, which escapes any raw HTML in the lesson source --%>
    <div class="lesson-body description">${body}</div>

    <c:choose>
      <c:when test="${not empty questions}">
        <section class="quiz card" id="quiz">
          <div class="card-head"><h2>🧠 Check your understanding</h2>
            <span class="small muted">Score ${passPercent}%+ to complete the lesson</span></div>
          <c:if test="${progress != null and progress.scored}">
            <p class="small muted">Your best so far: ${progress.quizScore}/${progress.quizTotal}
              (${progress.attempts} attempt${progress.attempts == 1 ? '' : 's'})</p>
          </c:if>
          <form class="quiz-form" method="post" action="${ctx}/learn/${topic.slug}/${lesson.slug}/quiz">
            <t:csrf/>
            <c:forEach var="q" items="${questions}" varStatus="row">
              <fieldset class="question" data-qid="${q.id}">
                <legend><span class="q-num">${row.count}</span><div class="q-prompt">${q.prompt}</div></legend>
                <c:forEach var="opt" items="${q.options}" varStatus="o">
                  <label class="option">
                    <input type="radio" name="q${q.id}" value="${o.index}" required>
                    <span class="opt-text">${opt}</span>
                  </label>
                </c:forEach>
                <div class="explain" hidden></div>
              </fieldset>
            </c:forEach>
            <div class="quiz-actions">
              <button class="btn btn-primary" type="submit">Check answers</button>
              <span class="quiz-result" role="status" aria-live="polite"></span>
            </div>
          </form>
        </section>
      </c:when>
      <c:when test="${not empty currentUser}">
        <div class="card quiz">
          <form class="complete-form" method="post" action="${ctx}/learn/${topic.slug}/${lesson.slug}/complete">
            <t:csrf/>
            <button class="btn btn-primary" type="submit" ${completed ? 'disabled' : ''}>${completed ? '✓ Completed' : 'Mark lesson complete'}</button>
          </form>
        </div>
      </c:when>
    </c:choose>

    <c:if test="${not empty practice}">
      <section class="section">
        <h2>💪 Practice what you learned</h2>
        <div class="practice-grid">
          <c:forEach var="p" items="${practice}">
            <a class="card practice-card" href="${ctx}/problems/${p.slug}">
              <div class="row spread"><strong><c:out value="${p.title}"/></strong><t:difficulty d="${p.difficulty}"/></div>
              <div class="small muted" style="margin-top:4px">${solved.contains(p.id) ? '✔ Solved' : 'Solve it →'}</div>
            </a>
          </c:forEach>
        </div>
      </section>
    </c:if>

    <nav class="lesson-nav" aria-label="Lesson navigation">
      <c:choose>
        <c:when test="${not empty prevLesson}">
          <a class="card nav-prev" href="${ctx}/learn/${prevLesson.topic.slug}/${prevLesson.slug}">
            <span class="muted small">← Previous</span><strong><c:out value="${prevLesson.title}"/></strong></a>
        </c:when>
        <c:otherwise><span></span></c:otherwise>
      </c:choose>
      <c:if test="${not empty nextLesson}">
        <a class="card nav-next" href="${ctx}/learn/${nextLesson.topic.slug}/${nextLesson.slug}">
          <span class="muted small">Next →</span><strong><c:out value="${nextLesson.title}"/></strong></a>
      </c:if>
    </nav>
  </article>
</div>
  </jsp:body>
</t:layout>
