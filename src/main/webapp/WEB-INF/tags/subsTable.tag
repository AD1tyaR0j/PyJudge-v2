<%-- A table of submissions, used on the history, profile and admin pages. --%>
<%@ tag pageEncoding="UTF-8" body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="pj" uri="http://pyjudge.com/functions" %>
<%@ attribute name="subs" required="true" type="java.util.List" %>
<%@ attribute name="showUser" type="java.lang.Boolean" %>
<div class="table-wrap">
  <table>
    <thead><tr>
      <th>#</th><c:if test="${showUser}"><th>User</th></c:if><th>Exercise</th><th>Verdict</th>
      <th>Tests</th><th>Grade</th><th>Runtime</th><th>Submitted (UTC)</th>
    </tr></thead>
    <tbody>
    <c:forEach var="s" items="${subs}">
      <tr>
        <td><a href="${ctx}/submissions/${s.id}">${s.id}</a></td>
        <c:if test="${showUser}"><td><a href="${ctx}/u/${fn:escapeXml(s.user.username)}"><c:out value="${s.user.username}"/></a></td></c:if>
        <td class="title"><a href="${ctx}/problems/${fn:escapeXml(s.problem.slug)}"><c:out value="${s.problem.title}"/></a></td>
        <td><t:verdict v="${s.verdict}"/></td>
        <td>${s.passed}/${s.total}</td>
        <td><t:grade g="${s.grade}"/></td>
        <td><c:choose><c:when test="${s.runtimeMs == null}">—</c:when><c:otherwise>${s.runtimeMs} ms</c:otherwise></c:choose></td>
        <td class="muted">${pj:dt(s.submittedAt)}</td>
      </tr>
    </c:forEach>
    <c:if test="${empty subs}">
      <tr><td colspan="8" class="muted">No submissions yet.</td></tr>
    </c:if>
    </tbody>
  </table>
</div>
