<t:layout title="New message" mainClass="container narrow">
<p class="small"><a href="${ctx}/messages">← Messages</a></p>
<h1>New message</h1>
<form method="post" action="${ctx}/messages/new" class="card">
  <t:csrf/>
  <div class="field">
    <label for="to">To</label>
    <select id="to" name="to" required>
      <option value="">Choose ${currentUser.student ? 'an instructor' : 'someone'}…</option>
      <c:forEach var="u" items="${contacts}">
        <option value="${fn:escapeXml(u.username)}" ${u.username == to ? 'selected' : ''}><c:out value="${u.displayName}"/> (${u.role.key})</option>
      </c:forEach>
    </select>
  </div>
  <c:if test="${not empty submission}">
    <input type="hidden" name="submission_id" value="${submission.id}">
    <div class="note">About submission <a href="${ctx}/submissions/${submission.id}">#${submission.id}</a> for “<c:out value="${submission.problem.title}"/>”</div>
  </c:if>
  <div class="field">
    <label for="body">Message</label>
    <textarea id="body" name="body" rows="6" maxlength="4000" required
      placeholder="${currentUser.student ? 'Describe what you tried and where you got stuck…' : 'Write your message…'}"></textarea>
  </div>
  <button class="btn btn-primary" type="submit" style="width:100%">Send</button>
</form>
</t:layout>
