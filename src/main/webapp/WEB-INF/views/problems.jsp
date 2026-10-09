<t:layout title="Exercises">
<h1>Exercises</h1>
<form class="filters" method="get">
  <input type="search" name="q" value="${fn:escapeXml(q)}" placeholder="Search exercises…">
  <select name="difficulty" onchange="this.form.submit()">
    <option value="">All difficulties</option>
    <c:forEach var="d" items="${difficulties}"><option ${d == selectedDifficulty ? 'selected' : ''}>${d.label}</option></c:forEach>
  </select>
  <select name="tag" onchange="this.form.submit()">
    <option value="">All topics</option>
    <c:forEach var="tg" items="${allTags}"><option ${tg == tag ? 'selected' : ''}><c:out value="${tg}"/></option></c:forEach>
  </select>
  <button class="btn" type="submit">Filter</button>
  <c:if test="${not empty q or not empty selectedDifficulty or not empty tag}"><a class="btn" href="${ctx}/problems">Clear</a></c:if>
</form>

<div class="table-wrap">
  <table>
    <thead><tr><th></th><th>#</th><th>Title</th><th>Difficulty</th><th>Acceptance</th><th>Topics</th></tr></thead>
    <tbody>
    <c:forEach var="p" items="${problems}">
      <c:set var="st" value="${stats[p.id]}"/>
      <tr>
        <td class="status-cell">
          <c:choose>
            <c:when test="${solved.contains(p.id)}"><span class="solved" title="Solved">✔</span></c:when>
            <c:when test="${attempted.contains(p.id)}"><span class="tried" title="Attempted">●</span></c:when>
          </c:choose>
        </td>
        <td class="muted">${p.id}</td>
        <td class="title"><a href="${ctx}/problems/${p.slug}"><c:out value="${p.title}"/></a></td>
        <td><t:difficulty d="${p.difficulty}"/></td>
        <td><c:choose><c:when test="${st != null and st.acceptance != null}">${st.acceptance}%</c:when><c:otherwise>—</c:otherwise></c:choose></td>
        <td class="title"><t:ptags problem="${p}"/></td>
      </tr>
    </c:forEach>
    <c:if test="${empty problems}">
      <tr><td colspan="6" class="muted">No exercises match your filters.</td></tr>
    </c:if>
    </tbody>
  </table>
</div>
</t:layout>
