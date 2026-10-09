<t:layout title="${isNew ? 'New challenge' : 'Edit challenge'}">
<p class="small"><a href="${ctx}/challenges">← Challenges</a></p>
<h1>${isNew ? 'New challenge' : 'Edit challenge'}</h1>
<form method="post" class="card">
  <t:csrf/>
  <div class="field"><label for="title">Title</label><input id="title" name="title" type="text" value="${fn:escapeXml(c.title)}" required maxlength="150"></div>
  <div class="field"><label for="description">Description <span class="muted small">(Markdown)</span></label>
    <textarea id="description" name="description" rows="4"><c:out value="${c.description}"/></textarea></div>
  <div class="grid grid-2">
    <div class="field"><label for="start_at">Starts (UTC)</label><input id="start_at" name="start_at" type="datetime-local" value="${pj:formDt(c.startAt)}" required></div>
    <div class="field"><label for="end_at">Ends (UTC)</label><input id="end_at" name="end_at" type="datetime-local" value="${pj:formDt(c.endAt)}" required></div>
  </div>
  <div class="field">
    <label>Exercises <span class="muted small">(approved exercises only)</span></label>
    <div class="check-list">
      <c:forEach var="p" items="${exercises}">
        <label><input type="checkbox" name="problem_ids" value="${p.id}" ${selected.contains(p.id) ? 'checked' : ''}> <c:out value="${p.title}"/> <t:difficulty d="${p.difficulty}"/></label>
      </c:forEach>
      <c:if test="${empty exercises}"><div class="empty">No approved exercises yet.</div></c:if>
    </div>
  </div>
  <button class="btn btn-primary" type="submit">${isNew ? 'Create challenge' : 'Save changes'}</button>
</form>
</t:layout>
