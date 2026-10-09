<t:layout title="Leaderboard">
<h1>Leaderboard</h1>
<p class="muted">Points per solved problem: Easy 10 · Medium 20 · Hard 40</p>
<div class="table-wrap">
  <table>
    <thead><tr><th>Rank</th><th>User</th><th>Solved</th><th>Score</th></tr></thead>
    <tbody>
    <c:forEach var="r" items="${rows}" varStatus="row">
      <tr <c:if test="${not empty currentUser and r.user.id == currentUser.id}">style="background:rgba(79,156,249,.08)"</c:if>>
        <td><c:choose>
          <c:when test="${row.count == 1}"><span class="medal">🥇</span></c:when>
          <c:when test="${row.count == 2}"><span class="medal">🥈</span></c:when>
          <c:when test="${row.count == 3}"><span class="medal">🥉</span></c:when>
          <c:otherwise>${row.count}</c:otherwise>
        </c:choose></td>
        <td><a href="${ctx}/u/${fn:escapeXml(r.user.username)}"><c:out value="${r.user.username}"/></a></td>
        <td>${r.solved}</td>
        <td><strong>${r.score}</strong></td>
      </tr>
    </c:forEach>
    <c:if test="${empty rows}">
      <tr><td colspan="4" class="muted">No one has solved a problem yet.</td></tr>
    </c:if>
    </tbody>
  </table>
</div>
</t:layout>
