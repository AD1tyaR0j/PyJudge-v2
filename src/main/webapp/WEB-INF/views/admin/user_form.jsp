<t:layout title="Edit ${target.username}" mainClass="container narrow">
<p class="small"><a href="${ctx}/admin/users">← All users</a></p>
<div class="row" style="margin-bottom:16px">
  <span class="avatar lg"><c:out value="${target.initial}"/></span>
  <div><h1 style="margin:0"><c:out value="${target.displayName}"/></h1><span class="muted">@<c:out value="${target.username}"/> · joined ${pj:dt(target.createdAt)}</span></div>
</div>
<form method="post" class="card">
  <t:csrf/>
  <div class="field"><label for="name">Full name</label><input id="name" name="name" type="text" value="${fn:escapeXml(target.name)}" required maxlength="100"></div>
  <div class="field"><label for="email">Email</label><input id="email" name="email" type="email" value="${fn:escapeXml(target.email)}" required></div>
  <div class="field"><label for="role">Role</label>
    <select id="role" name="role"><c:forEach var="r" items="${roles}"><option value="${r.key}" ${r == target.role ? 'selected' : ''}>${r.label}</option></c:forEach></select>
  </div>
  <div class="field"><label for="password">New password <span class="muted small">(leave blank to keep the current one)</span></label>
    <input id="password" name="password" type="password" minlength="8" autocomplete="new-password"></div>
  <div class="field"><label class="check"><input type="checkbox" name="is_blocked" value="1" ${target.blocked ? 'checked' : ''}> Block this account (they can't log in)</label></div>
  <button class="btn btn-primary" type="submit" style="width:100%">Save changes</button>
</form>
</t:layout>
