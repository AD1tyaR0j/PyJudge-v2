<%@ page isErrorPage="true" %>
<c:set var="status" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<c:set var="detail" value="${requestScope['jakarta.servlet.error.message']}"/>
<c:choose>
  <c:when test="${status == 404}"><c:set var="heading" value="Page not found"/></c:when>
  <c:when test="${status == 403}"><c:set var="heading" value="No access"/></c:when>
  <c:when test="${status == 400}"><c:set var="heading" value="That request didn't work"/></c:when>
  <c:when test="${status == 405}"><c:set var="heading" value="That action isn't available here"/></c:when>
  <c:otherwise><c:set var="heading" value="Something went wrong"/></c:otherwise>
</c:choose>
<c:if test="${empty site}">
  <%-- The app did not start (usually the database): there are no settings to build the normal layout with. --%>
  <!doctype html>
  <html lang="en"><head><meta charset="utf-8"><title>PyJudge is not running</title></head>
  <body style="font-family: system-ui, sans-serif; max-width: 640px; margin: 80px auto; padding: 0 20px">
    <h1>PyJudge could not start</h1>
    <p>The application did not finish starting, so it cannot serve pages yet. The most common cause is the
      database connection: check <code>db.url</code>, <code>db.user</code> and <code>db.password</code> in
      <code>application.properties</code>, make sure the database server is running, then restart.</p>
    <p>The server log has the exact error.</p>
  </body></html>
</c:if>
<c:if test="${not empty site}">
<t:layout title="${heading}" mainClass="container narrow">
  <div class="card" style="text-align:center; margin-top:48px">
    <div style="font-size:54px">🐍</div>
    <h1 style="margin:8px 0">${status} · <c:out value="${heading}"/></h1>
    <p class="muted">
      <c:choose>
        <c:when test="${status == 404}">That page doesn't exist, or you don't have access to it.</c:when>
        <c:when test="${status == 500}">Something went wrong on our side. Please try again.</c:when>
        <c:when test="${not empty detail}"><c:out value="${detail}"/></c:when>
        <c:otherwise>Please go back and try again.</c:otherwise>
      </c:choose>
    </p>
    <div class="row" style="justify-content:center; margin-top:16px">
      <a class="btn btn-primary" href="${ctx}/">Home</a>
      <a class="btn" href="${ctx}/problems">Exercises</a>
    </div>
  </div>
</t:layout>
</c:if>
