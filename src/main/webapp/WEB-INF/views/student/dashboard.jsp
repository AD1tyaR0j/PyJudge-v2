<t:layout title="My dashboard">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/static/css/learn.css"></jsp:attribute>
  <jsp:attribute name="scripts">
<t:chartScripts/>
<script>
  const T = ${pj:json(timeline)};
  Charts.line("progress-chart", { labels: T.labels, series: [{ label: "Solved", data: T.solved }] });
</script>
  </jsp:attribute>
  <jsp:body>
<div class="page-head">
  <div><h1>Hi <c:out value="${currentUser.displayName}"/> 👋</h1><p class="muted">Keep the streak going. Here's where you are.</p></div>
  <div class="actions">
    <a class="btn" href="${ctx}/problems">Exercises</a>
    <a class="btn btn-primary" href="${ctx}/learn">📚 Python course →</a>
  </div>
</div>

<div class="grid grid-4">
  <div class="card stat"><div class="num">${course.done}</div><div class="label">Lessons done</div><div class="sub">of ${course.total} in the course</div></div>
  <div class="card stat"><div class="num">${stats.solved}</div><div class="label">Exercises solved</div><div class="sub">of ${stats.total} · ${stats.score} pts</div></div>
  <div class="card stat"><div class="num">${stats.streak}</div><div class="label">Day streak 🔥</div><div class="sub"><c:choose><c:when test="${stats.rank != null}">rank #${stats.rank}</c:when><c:otherwise>not ranked yet</c:otherwise></c:choose></div></div>
  <div class="card stat"><div class="num"><c:choose><c:when test="${stats.avgGrade != null}">${stats.avgGrade}</c:when><c:otherwise>—</c:otherwise></c:choose></div><div class="label">Average grade</div>
    <div class="sub">quiz average <c:choose><c:when test="${course.quizAvg != null}">${course.quizAvg}%</c:when><c:otherwise>—</c:otherwise></c:choose></div></div>
</div>

<div class="section card continue-card" style="display:block">
  <div class="row spread">
    <div><div class="muted small">📚 Your Python course · ${course.percent}% complete</div>
      <c:choose>
        <c:when test="${not empty course.next}"><strong style="font-size:18px">Up next: <c:out value="${course.next.topic.icon}"/> <c:out value="${course.next.title}"/></strong></c:when>
        <c:otherwise><strong style="font-size:18px">🎓 You've completed every lesson!</strong></c:otherwise>
      </c:choose></div>
    <c:if test="${not empty course.next}">
      <a class="btn btn-primary" href="${ctx}/learn/${course.next.topic.slug}/${course.next.slug}">
        ${course.done > 0 ? 'Continue' : 'Start'} (${course.next.minutes} min) →</a>
    </c:if>
  </div>
  <div class="bar" style="margin-top:12px; height:10px"><span style="width:${course.percent}%; background: var(--grad-primary)"></span></div>
  <div class="row small muted" style="margin-top:10px; gap:6px 14px">
    <c:forEach var="tp" items="${course.topics}">
      <a href="${ctx}/learn/${tp.topic.slug}" title="${fn:escapeXml(tp.topic.title)}"
         style="color: ${tp.state == 'done' ? 'var(--green)' : 'var(--muted)'}"><c:out value="${tp.topic.icon}"/> ${tp.done}/${tp.total}</a>
    </c:forEach>
  </div>
</div>

<div class="section">
  <h2>🏅 Achievements <span class="muted small">(${badgesEarned}/${fn:length(badges)})</span></h2>
  <div class="badges">
    <c:forEach var="b" items="${badges}">
      <div class="badge-tile ${b.earned ? 'earned' : 'locked'}" title="${fn:escapeXml(b.desc)}">
        <span class="b-icon">${b.icon}</span><strong>${b.name}</strong><span>${b.desc}</span>
        <span style="display:block; margin-top:4px; color: ${b.earned ? 'var(--accent-2)' : 'var(--muted)'}">${b.earned ? '✓ Earned' : b.progress}</span>
      </div>
    </c:forEach>
  </div>
</div>

<div class="section grid grid-2">
  <div class="card">
    <div class="card-head"><h3>Exercises solved: last 30 days</h3></div>
    <div class="chart-box"><canvas id="progress-chart" aria-label="Cumulative exercises solved over the last 30 days"></canvas></div>
    <t:chartTable labels="${timeline.labels}" name1="Solved" values1="${timeline.solved}"/>
  </div>
  <div class="card">
    <h3>Progress by difficulty</h3>
    <c:forEach var="b" items="${byDifficulty}">
      <div style="margin-bottom:14px">
        <div class="row spread small"><span><t:difficulty d="${b.difficulty}"/></span><span class="muted">${b.solved} / ${b.total}</span></div>
        <div class="bar" style="margin-top:6px"><span style="width:${b.percent}%; background:var(--${b.colour})"></span></div>
      </div>
    </c:forEach>
    <h3 style="margin-top:22px">Up next</h3>
    <c:forEach var="p" items="${upNext}">
      <a class="list-item" href="${ctx}/problems/${p.slug}" style="color:inherit">
        <span>${inProgressIds.contains(p.id) ? '↻ ' : ''}<c:out value="${p.title}"/></span><t:difficulty d="${p.difficulty}"/></a>
    </c:forEach>
    <c:if test="${empty upNext}"><div class="empty">🏆 You've solved every exercise!</div></c:if>
  </div>
</div>

<div class="section">
  <h2>🏁 Challenges</h2>
  <div class="grid grid-2">
    <div class="card">
      <div class="card-head"><h3>Active</h3><a class="small" href="${ctx}/challenges">All challenges →</a></div>
      <c:forEach var="r" items="${active}">
        <a class="list-item" href="${ctx}/challenges/${r.challenge.id}" style="color:inherit; display:block">
          <div class="row spread"><strong><c:out value="${r.challenge.title}"/></strong><span class="meta">ends ${pj:dt(r.challenge.endAt)} UTC</span></div>
          <div class="bar" style="margin-top:8px"><span style="width:${r.percent}%; background:var(--accent)"></span></div>
          <div class="meta" style="margin-top:4px">${r.solved} / ${r.total} solved</div>
        </a>
      </c:forEach>
      <c:if test="${empty active}"><div class="empty">No active challenges right now.</div></c:if>
    </div>
    <div class="card">
      <h3>Completed</h3>
      <c:forEach var="r" items="${completed}">
        <a class="list-item" href="${ctx}/challenges/${r.challenge.id}" style="color:inherit">
          <span>🏅 <c:out value="${r.challenge.title}"/></span><span class="badge v-ac">${r.total}/${r.total}</span></a>
      </c:forEach>
      <c:if test="${empty completed}"><div class="empty">Finish every exercise in a challenge to earn a medal.</div></c:if>
    </div>
  </div>
</div>

<div class="section grid grid-2">
  <div class="card">
    <div class="card-head"><h3>📝 Feedback from instructors</h3><a class="small" href="${ctx}/submissions">All submissions →</a></div>
    <c:forEach var="s" items="${feedback}">
      <a class="list-item" href="${ctx}/submissions/${s.id}" style="color:inherit">
        <div style="min-width:0"><strong><c:out value="${s.problem.title}"/></strong>
          <div class="meta" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:330px"><c:out value="${empty s.feedback ? 'No written feedback' : s.feedback}"/></div></div>
        <t:grade g="${s.grade}"/>
      </a>
    </c:forEach>
    <c:if test="${empty feedback}"><div class="empty">Graded submissions and feedback will show up here.</div></c:if>
  </div>
  <div class="card">
    <div class="card-head"><h3>💬 Messages</h3>
      <div class="actions"><a class="btn btn-sm" href="${ctx}/messages/new">Ask an instructor</a></div></div>
    <c:forEach var="m" items="${messages}">
      <a class="list-item" href="${ctx}/messages/${fn:escapeXml(m.sender.username)}" style="color:inherit">
        <div style="min-width:0"><strong><c:out value="${m.sender.displayName}"/></strong><c:if test="${m.unread}"> <span class="count-badge">new</span></c:if>
          <div class="meta" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:330px"><c:out value="${m.body}"/></div></div>
        <span class="meta">${pj:dt(m.createdAt)}</span>
      </a>
    </c:forEach>
    <c:if test="${empty messages}"><div class="empty">No messages yet. Stuck on something? Ask an instructor.</div></c:if>
  </div>
</div>
  </jsp:body>
</t:layout>
