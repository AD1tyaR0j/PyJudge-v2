<t:layout title="${other.displayName} · Messages">
  <jsp:attribute name="scripts">
<script>const t = document.getElementById("thread"); t.scrollTop = t.scrollHeight;</script>
  </jsp:attribute>
  <jsp:body>
<p class="small"><a href="${ctx}/messages">← Messages</a></p>
<div class="row" style="margin-bottom:16px">
  <span class="avatar lg"><c:out value="${other.initial}"/></span>
  <div><h1 style="margin:0"><c:out value="${other.displayName}"/></h1>
    <span class="muted">@<c:out value="${other.username}"/></span> <t:role r="${other.role}"/></div>
</div>

<div class="card">
  <div class="thread" id="thread">
    <c:forEach var="m" items="${msgs}">
      <div class="bubble ${m.senderId == currentUser.id ? 'me' : 'them'}"><c:out value="${m.body}"/><c:if test="${not empty m.submission}"><br><a href="${ctx}/submissions/${m.submission.id}">📎 Submission #${m.submission.id}: <c:out value="${m.submission.problem.title}"/></a></c:if>
        <span class="time">${pj:dt(m.createdAt)} UTC<c:if test="${m.senderId == currentUser.id and not m.unread}"> · Seen</c:if></span></div>
    </c:forEach>
    <c:if test="${empty msgs}"><div class="empty">Say hello 👋</div></c:if>
  </div>
  <c:choose>
    <c:when test="${canReply}">
      <form method="post" style="margin-top:14px">
        <t:csrf/>
        <div class="row" style="align-items:flex-end; flex-wrap:nowrap">
          <textarea name="body" rows="2" maxlength="4000" required placeholder="Write a reply…" style="flex:1"></textarea>
          <button class="btn btn-primary" type="submit">Send</button>
        </div>
      </form>
    </c:when>
    <c:otherwise><p class="muted small" style="margin-bottom:0">You can't reply to this conversation.</p></c:otherwise>
  </c:choose>
</div>
  </jsp:body>
</t:layout>
