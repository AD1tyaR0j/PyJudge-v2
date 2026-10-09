<t:layout title="${c.title}">
<p class="small"><a href="${ctx}/challenges">← All challenges</a></p>
<div class="page-head">
  <div>
    <h1><c:out value="${c.title}"/></h1>
    <p class="muted">${pj:dt(c.startAt)} → ${pj:dt(c.endAt)} UTC
      · <span class="badge ${c.state == 'active' ? 'v-ac' : 'v-pending'}">${c.state == 'active' ? 'Active' : c.state == 'upcoming' ? 'Upcoming' : 'Ended'}</span>
      <c:if test="${not empty c.author}">· by <c:out value="${c.author.displayName}"/></c:if></p>
  </div>
  <c:if test="${c.editableBy(currentUser)}">
    <div class="actions">
      <a class="btn" href="${ctx}/challenges/${c.id}/edit">Edit</a>
      <form class="inline-form" method="post" action="${ctx}/challenges/${c.id}/delete" onsubmit="return confirm('Delete this challenge?')">
        <t:csrf/><button class="btn btn-danger" type="submit">Delete</button></form>
    </div>
  </c:if>
</div>
<c:if test="${not empty c.description}"><div class="card" style="margin-bottom:18px">${pj:md(c.description)}</div></c:if>

<c:if test="${not empty currentUser and not empty problems}">
  <div class="card" style="margin-bottom:18px">
    <div class="row spread"><strong>Your progress</strong><span>${fn:length(solved)} / ${fn:length(problems)}</span></div>
    <div class="bar" style="margin-top:8px; height:10px"><span style="width:${percent}%; background:var(--${percent == 100 ? 'green' : 'accent'})"></span></div>
    <c:choose>
      <c:when test="${percent == 100}"><p style="color:var(--green); margin:8px 0 0">🏅 Challenge complete!</p></c:when>
      <c:when test="${c.state == 'upcoming'}"><p class="muted small" style="margin:8px 0 0">Starts ${pj:dt(c.startAt)} UTC. Solutions only count during the challenge window.</p></c:when>
      <c:otherwise><p class="muted small" style="margin:8px 0 0">Only accepted solutions submitted during the challenge window count.</p></c:otherwise>
    </c:choose>
  </div>
</c:if>

<div class="grid grid-2">
  <div>
    <h2>Exercises</h2>
    <div class="card" style="padding:8px 16px">
      <c:forEach var="p" items="${problems}">
        <a class="list-item" href="${ctx}/problems/${p.slug}" style="color:inherit">
          <span><c:choose><c:when test="${solved.contains(p.id)}"><span class="solved">✔</span></c:when><c:otherwise><span class="muted">○</span></c:otherwise></c:choose> <c:out value="${p.title}"/></span>
          <t:difficulty d="${p.difficulty}"/>
        </a>
      </c:forEach>
    </div>
  </div>
  <div>
    <h2>Standings</h2>
    <div class="table-wrap">
      <table>
        <thead><tr><th>#</th><th>Student</th><th>Solved</th><th>Points</th></tr></thead>
        <tbody>
        <c:forEach var="r" items="${standings}" varStatus="row">
          <tr><td><c:choose>
              <c:when test="${row.count == 1}"><span class="medal">🥇</span></c:when>
              <c:when test="${row.count == 2}"><span class="medal">🥈</span></c:when>
              <c:when test="${row.count == 3}"><span class="medal">🥉</span></c:when>
              <c:otherwise>${row.count}</c:otherwise>
            </c:choose></td>
            <td><a href="${ctx}/u/${fn:escapeXml(r.user.username)}"><c:out value="${r.user.displayName}"/></a></td>
            <td>${r.solved} / ${fn:length(problems)}</td><td><strong>${r.score}</strong></td></tr>
        </c:forEach>
        <c:if test="${empty standings}">
          <tr><td colspan="4" class="empty">No solves yet. Be the first!</td></tr>
        </c:if>
        </tbody>
      </table>
    </div>
  </div>
</div>
</t:layout>
