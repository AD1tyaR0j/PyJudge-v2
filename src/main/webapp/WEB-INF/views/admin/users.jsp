<t:layout title="User management">
<p class="small"><a href="${ctx}/admin">← Admin dashboard</a></p>
<div class="page-head"><div><h1>User management</h1><p class="muted">Create, update, and delete accounts and roles.</p></div></div>

<form class="filters" method="get">
  <input type="search" name="q" value="${fn:escapeXml(q)}" placeholder="Search name, username, or email…">
  <select name="role" onchange="this.form.submit()">
    <option value="">All roles</option>
    <c:forEach var="r" items="${roles}"><option value="${r.key}" ${r == role ? 'selected' : ''}>${r.label}s</option></c:forEach>
  </select>
  <button class="btn" type="submit">Filter</button>
  <c:if test="${not empty q or not empty role}"><a class="btn" href="${ctx}/admin/users">Clear</a></c:if>
</form>

<div class="table-wrap">
  <table>
    <thead><tr><th>#</th><th>Name</th><th>Username</th><th>Email</th><th>Role</th><th>Status</th><th>Submissions</th><th>Last seen</th><th></th></tr></thead>
    <tbody>
    <c:forEach var="u" items="${users}">
      <tr>
        <td class="muted">${u.id}</td>
        <td><strong><c:out value="${u.displayName}"/></strong></td>
        <td><a href="${ctx}/u/${fn:escapeXml(u.username)}">@<c:out value="${u.username}"/></a></td>
        <td class="muted"><c:out value="${u.email}"/></td>
        <td><t:role r="${u.role}"/></td>
        <td><c:choose><c:when test="${u.blocked}"><span class="badge v-wa">Blocked</span></c:when><c:otherwise><span class="badge v-ac">Active</span></c:otherwise></c:choose></td>
        <td>${empty counts[u.id] ? 0 : counts[u.id]}</td>
        <td class="muted">${empty u.lastSeenAt ? '—' : pj:dt(u.lastSeenAt)}</td>
        <td class="actions">
          <a class="btn btn-sm" href="${ctx}/admin/users/${u.id}/edit">Edit</a>
          <c:if test="${u.id != currentUser.id}">
            <form class="inline-form" method="post" action="${ctx}/admin/users/${u.id}/delete"
                  onsubmit="return confirm('Delete this user and all their submissions and messages? This cannot be undone.')">
              <t:csrf/><button class="btn btn-sm btn-danger" type="submit">Delete</button>
            </form>
          </c:if>
        </td>
      </tr>
    </c:forEach>
    <c:if test="${empty users}">
      <tr><td colspan="9" class="muted">No users match.</td></tr>
    </c:if>
    </tbody>
  </table>
</div>

<form method="post" action="${ctx}/admin/users" class="card section" id="create">
  <t:csrf/>
  <h3>➕ Create a user</h3>
  <div class="grid grid-4">
    <div class="field"><label for="name">Full name</label><input id="name" name="name" type="text" value="${fn:escapeXml(form.name)}" required maxlength="100"></div>
    <div class="field"><label for="username">Username</label><input id="username" name="username" type="text" value="${fn:escapeXml(form.username)}" required pattern="[A-Za-z0-9_]{3,30}"></div>
    <div class="field"><label for="email">Email</label><input id="email" name="email" type="email" value="${fn:escapeXml(form.email)}" required></div>
    <div class="field"><label for="role">Role</label>
      <select id="role" name="role"><c:forEach var="r" items="${roles}"><option value="${r.key}" ${r.key == form.role ? 'selected' : ''}>${r.label}</option></c:forEach></select></div>
  </div>
  <div class="field" style="max-width:360px"><label for="password">Temporary password <span class="muted small">(8+ characters; share it with them securely)</span></label>
    <input id="password" name="password" type="password" required minlength="8" autocomplete="new-password"></div>
  <button class="btn btn-primary" type="submit">Create user</button>
</form>
</t:layout>
