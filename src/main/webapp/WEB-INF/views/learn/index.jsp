<t:layout title="Learn Python">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/static/css/learn.css"></jsp:attribute>
  <jsp:body>
<%-- The progress ring: a circle of radius 54 whose stroke is offset by the unfinished share. --%>
<c:set var="circumference" value="${2 * 3.14159 * 54}"/>
<section class="learn-hero">
  <div>
    <span class="eyebrow-sm">🐍 The PyJudge Python course</span>
    <h1>Learn Python, <span class="grad">one step at a time.</span></h1>
    <p class="muted">${fn:length(course.topics)} topics · ${course.total} hands-on lessons with runnable examples and quizzes.
      Every lesson links to exercises so you can practice right away.</p>
    <div class="row">
      <c:choose>
        <c:when test="${not empty course.next}">
          <a class="btn btn-primary btn-lg" href="${ctx}/learn/${course.next.topic.slug}/${course.next.slug}">
            ${course.done > 0 ? 'Continue learning' : 'Start the course'} →</a>
        </c:when>
        <c:otherwise>
          <a class="btn btn-primary btn-lg" href="${ctx}/problems?difficulty=Hard">🎓 Course complete: take on Hard exercises</a>
        </c:otherwise>
      </c:choose>
      <a class="btn btn-lg" href="${ctx}/problems">Practice exercises</a>
    </div>
  </div>
  <div class="ring-card">
    <svg class="ring" viewBox="0 0 140 140" role="img" aria-label="${course.percent}% of the course complete">
      <circle cx="70" cy="70" r="54" class="ring-bg"/>
      <circle cx="70" cy="70" r="54" class="ring-fg" style="--c: ${circumference}; --p: ${course.percent}"
              stroke-dasharray="${circumference}" stroke-dashoffset="${circumference * (1 - course.percent / 100)}"/>
      <text x="70" y="68" class="ring-num">${course.percent}%</text>
      <text x="70" y="88" class="ring-label">complete</text>
    </svg>
    <div class="ring-stats">
      <div><strong>${course.done}</strong><span>of ${course.total} lessons</span></div>
      <div><strong><c:choose><c:when test="${course.quizAvg != null}">${course.quizAvg}%</c:when><c:otherwise>—</c:otherwise></c:choose></strong><span>quiz average</span></div>
    </div>
    <c:if test="${empty currentUser}">
      <p class="small muted" style="margin:8px 0 0">
        <a href="${ctx}/login?next=${pj:url(path)}">Log in</a> to save your progress.</p>
    </c:if>
  </div>
</section>

<c:if test="${not empty course.next and course.done > 0}">
  <c:set var="nl" value="${course.next}"/>
  <a class="card continue-card" href="${ctx}/learn/${nl.topic.slug}/${nl.slug}">
    <span class="continue-icon"><c:out value="${nl.topic.icon}"/></span>
    <div><div class="muted small">Up next · <c:out value="${nl.topic.title}"/></div><strong><c:out value="${nl.title}"/></strong></div>
    <span class="muted small">${nl.minutes} min →</span>
  </a>
</c:if>

<h2 class="section">Course roadmap</h2>
<ol class="roadmap">
  <c:forEach var="tp" items="${course.topics}" varStatus="row">
    <li class="road-step state-${tp.state}" style="--i: ${row.index}">
      <span class="road-dot" aria-hidden="true">${tp.state == 'done' ? '✓' : row.count}</span>
      <a class="card road-card" href="${ctx}/learn/${tp.topic.slug}">
        <div class="road-head">
          <span class="road-icon"><c:out value="${tp.topic.icon}"/></span>
          <div>
            <h3><c:out value="${tp.topic.title}"/></h3>
            <div class="muted small"><c:out value="${tp.topic.level}"/> · ${tp.total} lessons · ~${tp.minutes} min</div>
          </div>
          <c:choose>
            <c:when test="${tp.state == 'done'}"><span class="badge v-ac">Done</span></c:when>
            <c:when test="${tp.state == 'active'}"><span class="badge st-pending">In progress</span></c:when>
          </c:choose>
        </div>
        <p class="muted"><c:out value="${tp.topic.summary}"/></p>
        <div class="bar"><span style="width:${tp.percent}%; background: var(--${tp.state == 'done' ? 'green' : 'accent'})"></span></div>
        <div class="small muted" style="margin-top:6px">${tp.done} / ${tp.total} lessons</div>
      </a>
    </li>
  </c:forEach>
</ol>
  </jsp:body>
</t:layout>
