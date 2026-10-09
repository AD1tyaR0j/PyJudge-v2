<t:layout title="Challenges">
<div class="page-head">
  <div><h1>Challenges</h1><p class="muted">Time-boxed sets of exercises. Solve them all before the deadline to complete a challenge.</p></div>
  <c:if test="${not empty currentUser and currentUser.author}">
    <a class="btn btn-primary" href="${ctx}/challenges/new">+ New challenge</a></c:if>
</div>

<c:forTokens var="state" items="active,upcoming,ended" delims=",">
  <c:if test="${not empty groups[state] or state == 'active'}">
  <div class="section">
    <h2>${state == 'active' ? '🔥 Active' : state == 'upcoming' ? '🗓 Upcoming' : '🏁 Ended'}</h2>
    <div class="grid grid-2">
      <c:forEach var="r" items="${groups[state]}">
        <c:set var="c" value="${r.challenge}"/>
        <a class="card" href="${ctx}/challenges/${c.id}" style="color:inherit">
          <div class="card-head"><h3><c:out value="${c.title}"/></h3>
            <c:if test="${r.completed}"><span class="badge v-ac">🏅 Completed</span></c:if></div>
          <p class="muted small" style="margin-top:0">${pj:dt(c.startAt)} → ${pj:dt(c.endAt)} UTC · ${r.total} exercises</p>
          <c:if test="${not empty currentUser and state != 'upcoming'}">
            <div class="bar"><span style="width:${r.percent}%; background:var(--${r.completed ? 'green' : 'accent'})"></span></div>
            <div class="small muted" style="margin-top:4px">${r.solved} / ${r.total} solved</div>
          </c:if>
        </a>
      </c:forEach>
      <c:if test="${empty groups[state]}">
        <div class="card empty">No active challenges right now.</div>
      </c:if>
    </div>
  </div>
  </c:if>
</c:forTokens>
</t:layout>
