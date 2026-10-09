<t:layout title="${isNew ? 'New lesson' : 'Edit '.concat(lesson.title)}">
<p class="small"><a href="${ctx}/instructor/lessons">← Lessons</a>
  <c:if test="${not isNew}"> · <a href="${ctx}/learn/${lesson.topic.slug}/${lesson.slug}">Preview lesson</a></c:if></p>
<div class="page-head">
  <h1>${isNew ? 'New lesson' : 'Edit lesson'}</h1>
  <c:if test="${not isNew}"><t:status s="${lesson.status}"/></c:if>
</div>

<c:choose>
  <c:when test="${not isNew and lesson.builtin}">
    <div class="note">📘 This is a built-in course lesson. Its source lives in <code>src/main/resources/curriculum/</code>, and the
      next course refresh at start-up will overwrite edits made here.</div>
  </c:when>
  <c:when test="${not isNew and lesson.status.key == 'pending'}">
    <div class="note">⏳ Waiting for an admin to approve it. Students can't see it yet.</div>
  </c:when>
  <c:when test="${not isNew and lesson.status.key == 'rejected'}">
    <div class="note bad">✖ Returned for changes.<c:if test="${not empty lesson.reviewNote}"><br><strong>Note:</strong> <c:out value="${lesson.reviewNote}"/></c:if>
      <br><span class="small muted">Save your changes to send it back for review.</span></div>
  </c:when>
  <c:when test="${isNew and currentUser.instructor and site.require_approval}">
    <div class="note">New lessons are reviewed by an admin before students can see them.</div>
  </c:when>
</c:choose>

<form method="post" class="card">
  <t:csrf/>
  <div class="grid grid-4">
    <div class="field" style="grid-column: span 2"><label for="title">Title</label>
      <input id="title" name="title" type="text" value="${fn:escapeXml(lesson.title)}" required maxlength="150"></div>
    <div class="field"><label for="topic_id">Topic</label>
      <select id="topic_id" name="topic_id" required>
        <option value="">Choose…</option>
        <c:forEach var="tp" items="${topics}"><option value="${tp.id}" ${tp.id == lesson.topicId ? 'selected' : ''}><c:out value="${tp.icon}"/> <c:out value="${tp.title}"/></option></c:forEach>
      </select></div>
    <div class="field"><label for="minutes">Reading time (min)</label>
      <input id="minutes" name="minutes" type="number" min="1" max="120" value="${lesson.minutes}"></div>
  </div>
  <div class="field"><label for="practice_tags">Practice tags <span class="muted small">(exercise tags to suggest, comma-separated)</span></label>
    <input id="practice_tags" name="practice_tags" type="text" value="${fn:escapeXml(lesson.practiceTags)}" placeholder="strings, loops"></div>
  <div class="field">
    <label for="content">Lesson content <span class="muted small">(Markdown)</span></label>
    <textarea id="content" name="content" rows="22" class="mono"><c:out value="${lesson.content}"/></textarea>
    <details class="small muted" style="margin-top:6px"><summary style="cursor:pointer">Formatting help</summary>
      <p>Write normal Markdown. A <code>```pyrun</code> block becomes a runnable playground. Inside it, put a line
        <code>### stdin</code> followed by any input the example should read. Use <code>```pyrun-error</code> for an example that's
        meant to raise an error, and <code>```python</code> for code that is shown but not runnable.</p>
    </details>
  </div>
  <button class="btn btn-primary" type="submit">${isNew ? 'Create lesson' : 'Save changes'}</button>
</form>

<c:if test="${not isNew}">
  <h2 id="quiz" class="section">Quiz questions (${fn:length(questions)})</h2>
  <p class="muted small">Students need half of these right to complete the lesson.</p>
  <c:forEach var="q" items="${questions}" varStatus="row">
    <div class="card" style="margin-bottom:12px">
      <div class="row spread small" style="margin-bottom:6px"><strong>Question ${row.count}</strong>
        <form method="post" action="${ctx}/studio/questions/${q.id}/delete" onsubmit="return confirm('Delete this question?')">
          <t:csrf/><button class="btn btn-sm btn-danger" type="submit">Delete</button></form></div>
      <div>${pj:md(q.prompt)}</div>
      <ol style="margin:6px 0"><c:forEach var="o" items="${q.options}" varStatus="opt">
        <li <c:if test="${opt.index == q.answer}">style="color:var(--green); font-weight:700"</c:if>><c:out value="${o}"/><c:if test="${opt.index == q.answer}"> ✓</c:if></li>
      </c:forEach></ol>
      <c:if test="${not empty q.explanation}"><div class="small muted">Explanation: <c:out value="${q.explanation}"/></div></c:if>
    </div>
  </c:forEach>

  <form method="post" action="${ctx}/studio/lessons/${lesson.id}/questions" class="card">
    <t:csrf/>
    <h3>Add a question</h3>
    <div class="field"><label for="prompt">Question <span class="muted small">(Markdown, code allowed)</span></label>
      <textarea id="prompt" name="prompt" rows="3" class="mono" required></textarea></div>
    <label>Options <span class="muted small">(select the correct one; leave extras empty)</span></label>
    <c:forEach var="i" begin="0" end="3">
      <div class="row" style="margin-bottom:8px; flex-wrap:nowrap">
        <input type="radio" name="answer" value="${i}" style="width:auto" aria-label="Option ${i + 1} is correct" ${i == 0 ? 'required' : ''}>
        <input type="text" name="option" placeholder="Option ${i + 1}">
      </div>
    </c:forEach>
    <div class="field"><label for="explanation">Explanation <span class="muted small">(shown after answering)</span></label>
      <input id="explanation" name="explanation" type="text"></div>
    <button class="btn btn-primary" type="submit">Add question</button>
  </form>

  <c:if test="${not lesson.builtin}">
    <form method="post" action="${ctx}/studio/lessons/${lesson.id}/delete" class="section"
          onsubmit="return confirm('Delete this lesson and all student progress on it?')">
      <t:csrf/><button class="btn btn-danger" type="submit">Delete lesson</button>
    </form>
  </c:if>
</c:if>
</t:layout>
