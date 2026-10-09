<t:layout title="Messages">
<div class="page-head">
  <div><h1>Messages</h1><p class="muted">${currentUser.student ? 'Ask your instructors questions and get feedback.' : 'Conversations with students and staff.'}</p></div>
  <c:if test="${site.messaging_enabled or currentUser.admin}"><a class="btn btn-primary" href="${ctx}/messages/new">✉ New message</a></c:if>
</div>
<c:if test="${not site.messaging_enabled}"><div class="note">Messaging is currently turned off by an administrator.</div></c:if>
<div class="card" style="padding:8px">
  <c:forEach var="c" items="${conversations}">
    <a class="convo ${c.unread > 0 ? 'unread' : ''}" href="${ctx}/messages/${fn:escapeXml(c.user.username)}">
      <span class="avatar"><c:out value="${c.user.initial}"/></span>
      <div class="body">
        <div class="row" style="gap:8px"><strong><c:out value="${c.user.displayName}"/></strong> <t:role r="${c.user.role}"/>
          <c:if test="${c.unread > 0}"><span class="count-badge">${c.unread}</span></c:if></div>
        <div class="preview">${c.last.senderId == currentUser.id ? 'You: ' : ''}<c:out value="${c.last.body}"/></div>
      </div>
      <span class="muted small">${pj:dt(c.last.createdAt)}</span>
    </a>
  </c:forEach>
  <c:if test="${empty conversations}"><div class="empty">No conversations yet.</div></c:if>
</div>
</t:layout>
