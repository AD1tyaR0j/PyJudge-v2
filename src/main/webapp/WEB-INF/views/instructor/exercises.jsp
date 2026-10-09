<t:layout title="My exercises">
<p class="small"><a href="${ctx}/dashboard">← Dashboard</a></p>
<div class="page-head">
  <div><h1>${currentUser.admin ? 'All exercises' : 'My exercises'}</h1><p class="muted">Create and manage coding exercises.</p></div>
  <a class="btn btn-primary" href="${ctx}/studio/exercises/new">+ New exercise</a>
</div>
<div class="table-wrap">
  <table>
    <thead><tr><th>Title</th><th>Difficulty</th><th>Status</th><th>Tests</th><th>Students</th><th>Acceptance</th><th></th></tr></thead>
    <tbody>
    <c:forEach var="p" items="${problems}">
      <c:set var="st" value="${stats[p.id]}"/>
      <c:set var="testCount" value="${empty tests[p.id] ? 0 : tests[p.id]}"/>
      <tr>
        <td class="title"><a href="${ctx}/problems/${p.slug}"><c:out value="${p.title}"/></a>
          <c:if test="${p.status.key == 'rejected' and not empty p.reviewNote}"><div class="small" style="color:var(--red)">“<c:out value="${p.reviewNote}"/>”</div></c:if></td>
        <td><t:difficulty d="${p.difficulty}"/></td>
        <td><t:status s="${p.status}"/></td>
        <td>${testCount}<c:if test="${testCount == 0}"> <span title="Add test cases" style="color:var(--orange)">⚠</span></c:if></td>
        <td>${st == null ? 0 : st.students}</td>
        <td><c:choose><c:when test="${st != null and st.acceptance != null}">${st.acceptance}%</c:when><c:otherwise>—</c:otherwise></c:choose></td>
        <td class="actions">
          <a class="btn btn-sm" href="${ctx}/studio/exercises/${p.id}/edit">Edit</a>
          <a class="btn btn-sm" href="${ctx}/instructor/submissions?exercise=${p.id}&amp;show=all">Submissions</a>
          <form class="inline-form" method="post" action="${ctx}/studio/exercises/${p.id}/delete"
                onsubmit="return confirm('Delete this exercise and all its submissions? This cannot be undone.')">
            <t:csrf/><button class="btn btn-sm btn-danger" type="submit">Delete</button>
          </form>
        </td>
      </tr>
    </c:forEach>
    <c:if test="${empty problems}">
      <tr><td colspan="7" class="empty">You haven't created any exercises yet. <a href="${ctx}/studio/exercises/new">Write your first one →</a></td></tr>
    </c:if>
    </tbody>
  </table>
</div>
</t:layout>
