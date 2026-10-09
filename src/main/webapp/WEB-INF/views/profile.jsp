<t:layout title="${profile.username}">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/static/css/learn.css"></jsp:attribute>
  <jsp:body>
<div class="row spread">
  <div>
    <div class="row">
      <span class="avatar lg"><c:out value="${profile.initial}"/></span>
      <div>
        <h1 style="margin:0"><c:out value="${profile.displayName}"/></h1>
        <p class="muted" style="margin:2px 0 0">@<c:out value="${profile.username}"/> · <t:role r="${profile.role}"/> · joined ${pj:monthYear(profile.createdAt)}</p>
      </div>
    </div>
    <c:if test="${not empty profile.bio}"><p style="max-width:640px; white-space:pre-wrap"><c:out value="${profile.bio}"/></p></c:if>
  </div>
</div>

<div class="grid grid-4" style="margin-bottom:24px">
  <div class="card stat"><div class="num">${fn:length(solved)}</div><div class="label">Solved</div></div>
  <div class="card stat"><div class="num">${score}</div><div class="label">Score</div></div>
  <div class="card stat"><div class="num"><c:choose><c:when test="${rank != null}">#${rank}</c:when><c:otherwise>—</c:otherwise></c:choose></div><div class="label">Rank</div></div>
  <div class="card stat"><div class="num"><c:choose><c:when test="${acceptance != null}">${acceptance}%</c:when><c:otherwise>—</c:otherwise></c:choose></div><div class="label">Acceptance</div></div>
</div>

<div class="grid" style="grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); align-items:start">
  <div class="card">
    <h3>Progress by difficulty</h3>
    <c:forEach var="b" items="${byDifficulty}">
      <div style="margin-bottom:12px">
        <div class="row spread small"><span>${b.difficulty.label}</span><span class="muted">${b.solved} / ${b.total}</span></div>
        <div class="bar"><span style="width:${b.percent}%; background:var(--${b.colour})"></span></div>
      </div>
    </c:forEach>
  </div>
  <div class="card">
    <h3>Submissions by verdict</h3>
    <c:forEach var="v" items="${verdictRows}">
      <div class="row spread small" style="padding:2px 0">
        <span class="v-${v.verdict.key}">${v.verdict.label}</span><span>${v.count}</span>
      </div>
    </c:forEach>
  </div>
</div>

<c:if test="${profile.student}">
<div class="card" style="margin-bottom:22px">
  <div class="row spread small"><strong>📚 Python course</strong>
    <span class="muted">${course.done} / ${course.total} lessons · quiz average <c:choose><c:when test="${course.quizAvg != null}">${course.quizAvg}%</c:when><c:otherwise>—</c:otherwise></c:choose></span></div>
  <div class="bar" style="margin-top:8px"><span style="width:${course.percent}%; background: var(--grad-primary)"></span></div>
</div>
</c:if>

<c:if test="${not empty badges}">
<h2>🏅 Achievements <span class="muted small">(${badgesEarned}/${fn:length(badges)})</span></h2>
<div class="badges" style="margin-bottom:24px">
  <c:forEach var="b" items="${earnedBadges}">
    <div class="badge-tile earned" title="${fn:escapeXml(b.desc)}"><span class="b-icon">${b.icon}</span><strong>${b.name}</strong><span>${b.desc}</span></div>
  </c:forEach>
  <c:if test="${empty earnedBadges}"><p class="muted">No achievements yet.</p></c:if>
</div>
</c:if>

<c:if test="${not empty authored}">
<h2>Exercises by <c:out value="${profile.displayName}"/></h2>
<div class="row" style="margin-bottom:20px">
  <c:forEach var="p" items="${authored}">
    <a class="card" style="padding:10px 14px" href="${ctx}/problems/${p.slug}"><c:out value="${p.title}"/> <t:difficulty d="${p.difficulty}"/></a>
  </c:forEach>
</div>
</c:if>

<h2>Solved exercises</h2>
<c:choose>
  <c:when test="${not empty solved}">
    <div class="row">
    <c:forEach var="p" items="${solved}">
      <a class="card" style="padding:10px 14px" href="${ctx}/problems/${p.slug}"><c:out value="${p.title}"/> <t:difficulty d="${p.difficulty}"/></a>
    </c:forEach>
    </div>
  </c:when>
  <c:otherwise><p class="muted">Nothing solved yet.</p></c:otherwise>
</c:choose>

<c:if test="${not empty recent}">
  <h2>Recent submissions</h2>
  <t:subsTable subs="${recent}"/>
</c:if>
  </jsp:body>
</t:layout>
