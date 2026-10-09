<t:layout title="Admin dashboard">
  <jsp:attribute name="scripts">
<t:chartScripts/>
<script>
  const A = ${pj:json(activity)};
  Charts.bar("subs-chart", { labels: A.labels, stacked: true, series: [
    { label: "Accepted", data: A.accepted }, { label: "Not accepted", data: A.other } ] });
  Charts.line("active-chart", { labels: A.labels, series: [
    { label: "Active students", data: A.active }, { label: "New sign-ups", data: A.signups } ] });
</script>
  </jsp:attribute>
  <jsp:body>
<div class="page-head">
  <div><h1>Admin dashboard</h1><p class="muted">Users, exercise content, and system settings.</p></div>
  <div class="actions">
    <a class="btn" href="${ctx}/admin/users">👥 Users</a>
    <a class="btn" href="${ctx}/admin/content">📚 Content</a>
    <a class="btn" href="${ctx}/admin/settings">⚙️ Settings</a>
    <a class="btn btn-primary" href="${ctx}/admin/approvals">Review approvals<c:if test="${stats.pending > 0}"> <span class="count-badge">${stats.pending}</span></c:if></a>
  </div>
</div>

<div class="grid grid-3">
  <a class="card stat" href="${ctx}/admin/users?role=student"><div class="num">${stats.students}</div><div class="label">Students</div></a>
  <a class="card stat" href="${ctx}/admin/users?role=instructor"><div class="num">${stats.instructors}</div><div class="label">Instructors</div></a>
  <a class="card stat" href="${ctx}/admin/content?status=approved"><div class="num">${stats.approved}</div><div class="label">Live exercises</div></a>
  <a class="card stat ${stats.pending > 0 ? 'warn' : ''}" href="${ctx}/admin/approvals"><div class="num">${stats.pending}</div><div class="label">Awaiting approval</div></a>
  <div class="card stat"><div class="num">${stats.submissions}</div><div class="label">Submissions</div></div>
  <div class="card stat"><div class="num">${stats.acceptance}%</div><div class="label">Acceptance rate</div></div>
</div>

<div class="section">
  <h2>User activity: last 14 days</h2>
  <div class="grid grid-2">
    <div class="card">
      <div class="card-head"><h3>Submissions per day</h3></div>
      <div class="chart-box"><canvas id="subs-chart" aria-label="Submissions per day, accepted vs not accepted"></canvas></div>
      <t:chartTable labels="${activity.labels}" name1="Accepted" values1="${activity.accepted}" name2="Not accepted" values2="${activity.other}"/>
    </div>
    <div class="card">
      <div class="card-head"><h3>Active students per day</h3></div>
      <div class="chart-box"><canvas id="active-chart" aria-label="Distinct students submitting per day"></canvas></div>
      <t:chartTable labels="${activity.labels}" name1="Active students" values1="${activity.active}" name2="New sign-ups" values2="${activity.signups}"/>
    </div>
  </div>
</div>

<div class="section grid grid-2">
  <div class="card">
    <div class="card-head"><h3>Content approval</h3><a class="small" href="${ctx}/admin/approvals">Open panel →</a></div>
    <c:forEach var="p" items="${pending}">
      <div class="list-item">
        <div><a href="${ctx}/problems/${p.slug}"><strong><c:out value="${p.title}"/></strong></a>
          <div class="meta">by <c:out value="${empty p.author ? 'deleted user' : p.author.displayName}"/> · ${empty tests[p.id] ? 0 : tests[p.id]} tests</div></div>
        <t:difficulty d="${p.difficulty}"/>
      </div>
    </c:forEach>
    <c:if test="${empty pending}"><div class="empty">🎉 Nothing waiting for review.</div></c:if>
  </div>
  <div class="card">
    <div class="card-head"><h3>Quick links</h3></div>
    <div class="list">
      <a class="list-item" href="${ctx}/admin/users#create"><span>➕ Create a user account</span><span class="muted">→</span></a>
      <a class="list-item" href="${ctx}/studio/exercises/new"><span>✍️ Write a new exercise</span><span class="muted">→</span></a>
      <a class="list-item" href="${ctx}/instructor/lessons"><span>📚 Manage course lessons (${stats.lessons} live · ${stats.lessonsCompleted} completions)</span><span class="muted">→</span></a>
      <a class="list-item" href="${ctx}/challenges/new"><span>🏁 Start a challenge</span><span class="muted">→</span></a>
      <a class="list-item" href="${ctx}/instructor/reports"><span>📊 Performance reports</span><span class="muted">→</span></a>
      <a class="list-item" href="${ctx}/instructor/submissions"><span>📝 Grade submissions</span><span class="muted">→</span></a>
    </div>
  </div>
</div>

<div class="section">
  <h2>Recent submissions</h2>
  <t:subsTable subs="${recent}" showUser="true"/>
</div>
  </jsp:body>
</t:layout>
