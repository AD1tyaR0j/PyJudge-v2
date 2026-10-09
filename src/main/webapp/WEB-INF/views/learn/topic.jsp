<t:layout title="${topic.title} · Learn">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/static/css/learn.css"></jsp:attribute>
  <jsp:body>
<p class="small"><a href="${ctx}/learn">← Course roadmap</a></p>
<div class="topic-head">
  <span class="topic-icon"><c:out value="${topic.icon}"/></span>
  <div>
    <div class="muted small">Topic ${number} of ${fn:length(course.topics)} · <c:out value="${topic.level}"/></div>
    <h1><c:out value="${topic.title}"/></h1>
    <div class="description">${pj:md(topic.description)}</div>
  </div>
</div>
<div class="card" style="margin-bottom:22px">
  <div class="row spread small"><strong>Your progress</strong><span class="muted">${info.done} / ${info.total} lessons · ~${info.minutes} min</span></div>
  <div class="bar" style="margin-top:8px; height:10px"><span style="width:${info.percent}%; background:var(--${info.state == 'done' ? 'green' : 'accent'})"></span></div>
</div>

<div class="grid grid-2">
  <div>
    <h2>Lessons</h2>
    <ol class="lesson-list">
      <c:forEach var="lesson" items="${info.lessons}" varStatus="row">
        <c:set var="p" value="${course.progress[lesson.id]}"/>
        <c:set var="done" value="${p != null and p.completed}"/>
        <li style="--i: ${row.index}">
          <a class="card lesson-row ${done ? 'is-done' : ''}" href="${ctx}/learn/${topic.slug}/${lesson.slug}">
            <span class="lesson-num">${done ? '✓' : row.count}</span>
            <div class="grow">
              <strong><c:out value="${lesson.title}"/></strong>
              <div class="muted small">${lesson.minutes} min<c:if test="${lesson.questionCount > 0}"> · ${lesson.questionCount}-question quiz</c:if>
                <c:if test="${p != null and p.scored}"> · best ${p.quizScore}/${p.quizTotal}</c:if></div>
            </div>
            <span class="muted">→</span>
          </a>
        </li>
      </c:forEach>
    </ol>
  </div>
  <div>
    <h2>Practice this topic</h2>
    <div class="card" style="padding:8px 16px">
      <c:forEach var="p" items="${practice}">
        <a class="list-item" href="${ctx}/problems/${p.slug}" style="color:inherit">
          <span><c:if test="${solved.contains(p.id)}"><span class="solved">✔</span> </c:if><c:out value="${p.title}"/></span><t:difficulty d="${p.difficulty}"/></a>
      </c:forEach>
      <c:if test="${empty practice}"><div class="empty">No matching exercises yet.</div></c:if>
    </div>
  </div>
</div>
  </jsp:body>
</t:layout>
