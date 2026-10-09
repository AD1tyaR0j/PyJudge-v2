<t:layout title="Lessons">
<p class="small"><a href="${ctx}/dashboard">← Dashboard</a></p>
<div class="page-head">
  <div><h1>${currentUser.admin ? 'Course lessons' : 'My lessons'}</h1>
    <p class="muted">Write tutorials with runnable examples and quizzes. ${site.require_approval ? 'Instructor lessons are reviewed by an admin before publishing.' : ''}</p></div>
  <a class="btn btn-primary" href="${ctx}/studio/lessons/new">+ New lesson</a>
</div>
<div class="table-wrap">
  <table>
    <thead><tr><th>Topic</th><th>Lesson</th><th>Source</th><th>Status</th><th>Quiz</th><th>Completed by</th><th>Avg quiz</th><th></th></tr></thead>
    <tbody>
    <c:forEach var="l" items="${lessons}">
      <tr>
        <td><c:out value="${l.topic.icon}"/> <c:out value="${l.topic.title}"/></td>
        <td class="title"><a href="${ctx}/learn/${l.topic.slug}/${l.slug}"><c:out value="${l.title}"/></a>
          <c:if test="${l.status.key == 'rejected' and not empty l.reviewNote}"><div class="small" style="color:var(--red)">“<c:out value="${l.reviewNote}"/>”</div></c:if></td>
        <td class="muted"><c:choose><c:when test="${l.builtin}">Course</c:when><c:otherwise><c:out value="${empty l.author ? '—' : l.author.displayName}"/></c:otherwise></c:choose></td>
        <td><t:status s="${l.status}"/></td>
        <td>${l.questionCount} Q</td>
        <td>${empty completions[l.id] ? 0 : completions[l.id]}</td>
        <td><c:choose><c:when test="${avgQuiz[l.id] != null}">${avgQuiz[l.id]}%</c:when><c:otherwise>—</c:otherwise></c:choose></td>
        <td><c:if test="${l.editableBy(currentUser)}"><a class="btn btn-sm" href="${ctx}/studio/lessons/${l.id}/edit">Edit</a></c:if></td>
      </tr>
    </c:forEach>
    <c:if test="${empty lessons}">
      <tr><td colspan="8" class="empty">You haven't written any lessons yet. <a href="${ctx}/studio/lessons/new">Write your first one →</a></td></tr>
    </c:if>
    </tbody>
  </table>
</div>
</t:layout>
