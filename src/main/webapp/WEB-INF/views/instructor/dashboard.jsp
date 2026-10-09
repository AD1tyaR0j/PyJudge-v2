<t:layout title="Instructor dashboard">
<div class="page-head">
  <div><h1>Welcome back, <c:out value="${currentUser.displayName}"/> 👋</h1><p class="muted">Your exercises, students, and messages at a glance.</p></div>
  <div class="actions">
    <a class="btn" href="${ctx}/challenges/new">🏁 New challenge</a>
    <a class="btn" href="${ctx}/instructor/lessons">📚 My lessons</a>
    <a class="btn" href="${ctx}/studio/lessons/new">+ New lesson</a>
    <a class="btn btn-primary" href="${ctx}/studio/exercises/new">+ New exercise</a>
  </div>
</div>

<div class="grid grid-4">
  <a class="card stat" href="${ctx}/instructor/exercises"><div class="num">${stats.exercises}</div><div class="label">My exercises</div>
    <div class="sub">${stats.approved} live · ${stats.pending} pending</div></a>
  <a class="card stat ${stats.toGrade > 0 ? 'warn' : ''}" href="${ctx}/instructor/submissions"><div class="num">${stats.toGrade}</div><div class="label">To grade</div></a>
  <a class="card stat" href="${ctx}/instructor/reports"><div class="num">${stats.students}</div><div class="label">Students reached</div></a>
  <a class="card stat" href="${ctx}/messages"><div class="num">${empty unreadMessages ? 0 : unreadMessages}</div><div class="label">Unread messages</div></a>
</div>

<c:if test="${not empty rejected}">
  <div class="section">
    <c:forEach var="p" items="${rejected}">
      <div class="note bad">✖ <a href="${ctx}/studio/exercises/${p.id}/edit"><strong><c:out value="${p.title}"/></strong></a> was returned for changes.
        <c:if test="${not empty p.reviewNote}"><span class="muted">“<c:out value="${p.reviewNote}"/>”</span></c:if></div>
    </c:forEach>
  </div>
</c:if>

<div class="section grid grid-2">
  <div class="card">
    <div class="card-head"><h3>📝 Student submissions to grade</h3><a class="small" href="${ctx}/instructor/submissions">See all →</a></div>
    <c:forEach var="s" items="${toGrade}">
      <a class="list-item" href="${ctx}/submissions/${s.id}" style="color:inherit">
        <div><strong><c:out value="${s.user.displayName}"/></strong> · <c:out value="${s.problem.title}"/>
          <div class="meta">${pj:dt(s.submittedAt)} UTC · ${s.passed}/${s.total} tests</div></div>
        <t:verdict v="${s.verdict}"/>
      </a>
    </c:forEach>
    <c:if test="${empty toGrade}"><div class="empty">Nothing to grade right now.</div></c:if>
  </div>
  <div class="card">
    <div class="card-head"><h3>💬 Student messages</h3><a class="small" href="${ctx}/messages">Inbox →</a></div>
    <c:forEach var="m" items="${inbox}">
      <a class="list-item" href="${ctx}/messages/${fn:escapeXml(m.sender.username)}" style="color:inherit">
        <div style="min-width:0"><strong><c:out value="${m.sender.displayName}"/></strong><c:if test="${m.unread}"> <span class="count-badge">new</span></c:if>
          <div class="meta" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:340px"><c:out value="${m.body}"/></div></div>
        <span class="meta">${pj:dt(m.createdAt)}</span>
      </a>
    </c:forEach>
    <c:if test="${empty inbox}"><div class="empty">No messages yet.</div></c:if>
  </div>
</div>
</t:layout>
