<%-- The page shell shared by every screen: head, navigation bar, flash messages and footer scripts. --%>
<%@ tag pageEncoding="UTF-8" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="bodyClass" %>
<%@ attribute name="mainClass" %>
<%@ attribute name="flashClass" %>
<%@ attribute name="hideFlashes" type="java.lang.Boolean" %>
<%@ attribute name="head" fragment="true" %>
<%@ attribute name="scripts" fragment="true" %>
<c:set var="user" value="${requestScope.currentUser}"/>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="csrf-token" content="${csrfToken}">
  <meta name="app-base" content="${ctx}">
  <title><c:out value="${title}"/> · <c:out value="${site.site_name}"/></title>
  <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🐍</text></svg>">
  <script>try { var m = localStorage.getItem("pyjudge:motion"); if (m) document.documentElement.dataset.motion = m; } catch (e) {}</script>
  <link rel="stylesheet" href="${ctx}/static/css/style.css">
  <link rel="stylesheet" href="${ctx}/static/css/motion.css">
  <script src="${ctx}/static/js/ui.js" defer></script>
  <jsp:invoke fragment="head"/>
</head>
<body class="${bodyClass}">
  <div class="backdrop" aria-hidden="true">
    <span class="orb o1"></span><span class="orb o2"></span><span class="orb o3"></span>
    <span class="stars"></span><span class="grid-floor"></span>
  </div>
  <header class="nav">
    <a class="brand" href="${ctx}/">
      <c:choose>
        <c:when test="${site.site_name == 'PyJudge'}">Py<span>Judge</span></c:when>
        <c:otherwise><c:out value="${site.site_name}"/></c:otherwise>
      </c:choose>
    </a>
    <nav>
      <button class="motion-toggle" id="motion-toggle" type="button" aria-pressed="false" title="Toggle animations">✨</button>
      <c:if test="${not empty user}">
        <a href="${ctx}/dashboard" class="${section == 'dashboard' ? 'active' : ''}">${user.admin ? 'Admin' : user.instructor ? 'Instructor' : 'Dashboard'}</a>
      </c:if>
      <a href="${ctx}/learn" class="${section == 'learn' ? 'active' : ''}">Learn</a>
      <a href="${ctx}/problems" class="${section == 'problems' ? 'active' : ''}">Exercises</a>
      <a href="${ctx}/challenges" class="${section == 'challenges' ? 'active' : ''}">Challenges</a>
      <c:if test="${site.leaderboard_enabled}">
        <a href="${ctx}/leaderboard" class="${section == 'leaderboard' ? 'active' : ''}">Leaderboard</a>
      </c:if>
      <c:choose>
        <c:when test="${not empty user}">
          <c:if test="${site.messaging_enabled or user.admin}">
            <a href="${ctx}/messages" class="${section == 'messages' ? 'active' : ''}">Messages<c:if test="${unreadMessages > 0}"> <span class="count-badge">${unreadMessages}</span></c:if></a>
          </c:if>
          <details class="user-menu">
            <summary><span class="avatar"><c:out value="${user.initial}"/></span><span class="user-name"><c:out value="${user.displayName}"/></span></summary>
            <div class="menu">
              <div class="menu-head"><strong><c:out value="${user.displayName}"/></strong><t:role r="${user.role}"/></div>
              <a href="${ctx}/u/${fn:escapeXml(user.username)}">Public profile</a>
              <a href="${ctx}/submissions">My submissions</a>
              <a href="${ctx}/settings">Account settings</a>
              <form method="post" action="${ctx}/logout">
                <t:csrf/>
                <button type="submit">Log out</button>
              </form>
            </div>
          </details>
        </c:when>
        <c:otherwise>
          <a href="${ctx}/login">Log in</a>
          <c:if test="${site.registration_open}"><a class="btn btn-primary btn-sm" href="${ctx}/register">Sign up</a></c:if>
        </c:otherwise>
      </c:choose>
    </nav>
  </header>
  <c:if test="${not empty site.announcement}"><div class="announcement">📣 <c:out value="${site.announcement}"/></div></c:if>

  <main class="${mainClass == null ? 'container' : mainClass}">
    <c:if test="${not hideFlashes and not empty flashes}">
      <div class="${flashClass}">
        <c:forEach var="f" items="${flashes}">
          <div class="flash flash-${f[0]}"><c:out value="${f[1]}"/></div>
        </c:forEach>
      </div>
    </c:if>
    <jsp:doBody/>
  </main>
  <jsp:invoke fragment="scripts"/>
</body>
</html>
