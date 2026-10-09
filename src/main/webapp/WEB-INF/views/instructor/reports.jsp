<t:layout title="Performance reports">
  <jsp:attribute name="scripts">
<c:if test="${chartSize > 0}">
<t:chartScripts/>
<script>
  const R = ${pj:json(chart)};
  Charts.bar("acc-chart", { labels: R.labels, horizontal: true, max: 100, suffix: "%",
    series: [{ label: "Acceptance", data: R.acceptance }] });
</script>
</c:if>
  </jsp:attribute>
  <jsp:body>
<p class="small"><a href="${ctx}/dashboard">← Dashboard</a></p>
<div class="page-head"><div><h1>Performance reports</h1>
  <p class="muted">How students are doing on ${currentUser.admin ? 'all live exercises' : 'your live exercises'}.</p></div></div>

<c:if test="${chartSize > 0}">
<div class="card">
  <div class="card-head"><h3>Acceptance rate by exercise</h3></div>
  <div class="chart-box ${chartSize > 6 ? 'tall' : ''}"><canvas id="acc-chart" aria-label="Acceptance rate by exercise"></canvas></div>
  <t:chartTable labels="${chart.labels}" name1="Acceptance" values1="${chart.acceptance}" suffix="%"/>
</div>
</c:if>

<div class="section">
  <h2>Exercises</h2>
  <div class="table-wrap">
    <table>
      <thead><tr><th>Exercise</th><th>Difficulty</th><th>Students</th><th>Solved by</th><th>Submissions</th><th>Acceptance</th><th>Avg grade</th></tr></thead>
      <tbody>
      <c:forEach var="r" items="${exerciseRows}">
        <tr>
          <td class="title"><a href="${ctx}/instructor/submissions?exercise=${r.problem.id}&amp;show=all"><c:out value="${r.problem.title}"/></a></td>
          <td><t:difficulty d="${r.problem.difficulty}"/></td>
          <td>${r.students}</td>
          <td>${r.solvers}</td>
          <td>${r.submissions}</td>
          <td><c:choose><c:when test="${r.acceptance != null}">${r.acceptance}%</c:when><c:otherwise>—</c:otherwise></c:choose></td>
          <td><t:grade g="${r.avgGrade}"/></td>
        </tr>
      </c:forEach>
      <c:if test="${empty exerciseRows}">
        <tr><td colspan="7" class="empty">No live exercises yet.</td></tr>
      </c:if>
      </tbody>
    </table>
  </div>
</div>

<div class="section">
  <h2>Students</h2>
  <div class="table-wrap">
    <table>
      <thead><tr><th>Student</th><th>Progress</th><th>Solved</th><th>Attempted</th><th>Submissions</th><th>Avg grade</th><th>Last active</th><th></th></tr></thead>
      <tbody>
      <c:forEach var="r" items="${studentRows}">
        <tr>
          <td><a href="${ctx}/u/${fn:escapeXml(r.user.username)}"><strong><c:out value="${r.user.displayName}"/></strong></a></td>
          <td style="min-width:140px"><div class="bar"><span style="width:${r.progress}%; background:var(--accent)"></span></div>
            <span class="small muted">${r.progress}%</span></td>
          <td>${r.solved}</td>
          <td>${r.attempted}</td>
          <td>${r.submissions}</td>
          <td><t:grade g="${r.avgGrade}"/></td>
          <td class="muted">${pj:dt(r.last)}</td>
          <td><a class="btn btn-sm" href="${ctx}/messages/${fn:escapeXml(r.user.username)}">Message</a></td>
        </tr>
      </c:forEach>
      <c:if test="${empty studentRows}">
        <tr><td colspan="8" class="empty">No student activity yet.</td></tr>
      </c:if>
      </tbody>
    </table>
  </div>
</div>
  </jsp:body>
</t:layout>
