<t:layout title="Exercise content">
<p class="small"><a href="${ctx}/admin">← Admin dashboard</a></p>
<div class="page-head">
  <div><h1>Exercise content</h1><p class="muted">Every exercise on the platform and its approval status.</p></div>
  <a class="btn btn-primary" href="${ctx}/studio/exercises/new">+ New exercise</a>
</div>

<div class="seg" style="margin-bottom:16px">
  <a href="${ctx}/admin/content" class="${empty status ? 'on' : ''}">All (${totalCount})</a>
  <c:forEach var="s" items="${statuses}">
    <a href="${ctx}/admin/content?status=${s.key}" class="${s == status ? 'on' : ''}">${s.label} (${empty counts[s.key] ? 0 : counts[s.key]})</a>
  </c:forEach>
</div>

<div class="table-wrap">
  <table>
    <thead><tr><th>#</th><th>Title</th><th>Author</th><th>Difficulty</th><th>Status</th><th>Tests</th><th>Updated</th><th></th></tr></thead>
    <tbody>
    <c:forEach var="p" items="${problems}">
      <tr>
        <td class="muted">${p.id}</td>
        <td class="title"><a href="${ctx}/problems/${p.slug}"><c:out value="${p.title}"/></a></td>
        <td><c:out value="${empty p.author ? '—' : p.author.displayName}"/></td>
        <td><t:difficulty d="${p.difficulty}"/></td>
        <td><t:status s="${p.status}"/></td>
        <td>${empty tests[p.id] ? 0 : tests[p.id]}</td>
        <td class="muted">${pj:dt(p.updatedAt)}</td>
        <td class="actions">
          <c:if test="${not p.approved}">
            <form class="inline-form" method="post" action="${ctx}/admin/exercises/${p.id}/review">
              <t:csrf/><input type="hidden" name="decision" value="approve"><button class="btn btn-sm btn-success" type="submit">Approve</button>
            </form>
          </c:if>
          <a class="btn btn-sm" href="${ctx}/studio/exercises/${p.id}/edit">Edit</a>
          <form class="inline-form" method="post" action="${ctx}/studio/exercises/${p.id}/delete"
                onsubmit="return confirm('Delete this exercise and all its submissions? This cannot be undone.')">
            <t:csrf/><button class="btn btn-sm btn-danger" type="submit">Delete</button>
          </form>
        </td>
      </tr>
    </c:forEach>
    <c:if test="${empty problems}">
      <tr><td colspan="8" class="muted">No exercises here.</td></tr>
    </c:if>
    </tbody>
  </table>
</div>
</t:layout>
