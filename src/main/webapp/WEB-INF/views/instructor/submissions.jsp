<t:layout title="Student submissions">
<p class="small"><a href="${ctx}/dashboard">← Dashboard</a></p>
<div class="page-head"><div><h1>Student submissions</h1><p class="muted">Review, grade, and give feedback.</p></div></div>

<c:set var="exerciseParam" value="${fn:escapeXml(exerciseQuery)}"/>
<form class="filters" method="get">
  <div class="seg">
    <a href="${ctx}/instructor/submissions?show=ungraded${exerciseParam}" class="${show == 'ungraded' ? 'on' : ''}">To grade</a>
    <a href="${ctx}/instructor/submissions?show=graded${exerciseParam}" class="${show == 'graded' ? 'on' : ''}">Graded</a>
    <a href="${ctx}/instructor/submissions?show=all${exerciseParam}" class="${show == 'all' ? 'on' : ''}">All</a>
  </div>
  <input type="hidden" name="show" value="${fn:escapeXml(show)}">
  <select name="exercise" onchange="this.form.submit()">
    <option value="">All exercises</option>
    <c:forEach var="p" items="${exercises}"><option value="${p.id}" ${p.id == problemId ? 'selected' : ''}><c:out value="${p.title}"/></option></c:forEach>
  </select>
</form>

<div class="table-wrap">
  <table>
    <thead><tr><th>#</th><th>Student</th><th>Exercise</th><th>Verdict</th><th>Tests</th><th>Grade</th><th>Submitted (UTC)</th><th></th></tr></thead>
    <tbody>
    <c:forEach var="s" items="${pagination.items}">
      <tr>
        <td class="muted">${s.id}</td>
        <td><strong><c:out value="${s.user.displayName}"/></strong> <span class="muted small">@<c:out value="${s.user.username}"/></span></td>
        <td class="title"><c:out value="${s.problem.title}"/></td>
        <td><t:verdict v="${s.verdict}"/></td>
        <td>${s.passed}/${s.total}</td>
        <td><t:grade g="${s.grade}"/></td>
        <td class="muted">${pj:dt(s.submittedAt)}</td>
        <td><a class="btn btn-sm ${s.grade == null ? 'btn-primary' : ''}" href="${ctx}/submissions/${s.id}">${s.grade == null ? 'Grade' : 'Review'}</a></td>
      </tr>
    </c:forEach>
    <c:if test="${empty pagination.items}">
      <tr><td colspan="8" class="empty">${show == 'ungraded' ? '🎉 Everything is graded.' : 'No submissions yet.'}</td></tr>
    </c:if>
    </tbody>
  </table>
</div>
<t:pager page="${pagination}" base="${ctx}/instructor/submissions?show=${fn:escapeXml(show)}${exerciseParam}&amp;page="/>
</t:layout>
