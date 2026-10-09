<t:layout title="${isNew ? 'New exercise' : 'Edit '.concat(problem.title)}">
<p class="small">
  <c:choose>
    <c:when test="${currentUser.admin}"><a href="${ctx}/admin/content">← Exercise content</a></c:when>
    <c:otherwise><a href="${ctx}/instructor/exercises">← My exercises</a></c:otherwise>
  </c:choose>
  <c:if test="${not isNew}"> · <a href="${ctx}/problems/${problem.slug}">Preview &amp; try it</a></c:if></p>
<div class="page-head">
  <h1>${isNew ? 'New exercise' : 'Edit exercise'}</h1>
  <c:if test="${not isNew}"><t:status s="${problem.status}"/></c:if>
</div>

<c:choose>
  <c:when test="${not isNew and problem.status.key == 'pending'}">
    <div class="note">⏳ Waiting for an admin to approve it. Students can't see it yet.</div>
  </c:when>
  <c:when test="${not isNew and problem.status.key == 'rejected'}">
    <div class="note bad">✖ Returned for changes<c:if test="${not empty problem.reviewer}"> by <c:out value="${problem.reviewer.displayName}"/></c:if>.
      <c:if test="${not empty problem.reviewNote}"><br><strong>Note:</strong> <c:out value="${problem.reviewNote}"/></c:if>
      <br><span class="small muted">Save your changes to send it back for review.</span></div>
  </c:when>
  <c:when test="${not isNew and currentUser.instructor and site.require_approval}">
    <div class="note ok">✔ Live for students. Editing the details will send it back for approval.</div>
  </c:when>
  <c:when test="${isNew and currentUser.instructor and site.require_approval}">
    <div class="note">New exercises are reviewed by an admin before students can see them.</div>
  </c:when>
</c:choose>

<form method="post" class="card">
  <t:csrf/>
  <div class="field">
    <label for="title">Title</label>
    <input id="title" name="title" type="text" value="${fn:escapeXml(problem.title)}" required maxlength="150">
  </div>
  <div class="grid grid-4">
    <div class="field">
      <label for="difficulty">Difficulty</label>
      <select id="difficulty" name="difficulty">
        <c:forEach var="d" items="${difficulties}"><option ${d == problem.difficulty ? 'selected' : ''}>${d.label}</option></c:forEach>
      </select>
    </div>
    <div class="field">
      <label for="time_limit">Time limit (s)</label>
      <input id="time_limit" name="time_limit" type="number" min="1" max="10" value="${problem.timeLimit}">
    </div>
    <div class="field">
      <label for="memory_limit">Memory limit (MB)</label>
      <input id="memory_limit" name="memory_limit" type="number" min="16" max="1024" value="${problem.memoryLimit}">
    </div>
    <div class="field">
      <label for="tags">Tags <span class="muted small">(comma-separated)</span></label>
      <input id="tags" name="tags" type="text" value="${fn:escapeXml(problem.tags)}" placeholder="arrays, math">
    </div>
  </div>
  <div class="field">
    <label for="description">Statement <span class="muted small">(Markdown)</span></label>
    <textarea id="description" name="description" rows="12" class="mono"><c:out value="${problem.description}"/></textarea>
  </div>
  <div class="field">
    <label for="starter_code">Starter code</label>
    <textarea id="starter_code" name="starter_code" rows="4" class="mono"><c:out value="${problem.starterCode}"/></textarea>
  </div>
  <button class="btn btn-primary" type="submit">${isNew ? 'Create exercise' : 'Save changes'}</button>
</form>

<c:if test="${not isNew}">
  <h2 id="tests">Test cases (${fn:length(testCases)})</h2>
  <p class="muted small">Sample cases are shown on the problem page. All cases (sample and hidden) are used for judging. Output is compared after trimming trailing whitespace on each line.</p>
  <c:forEach var="tc" items="${testCases}" varStatus="row">
    <div class="card" style="margin-bottom:12px">
      <div class="row spread small" style="margin-bottom:8px">
        <strong>Case ${row.count} <c:choose><c:when test="${tc.sample}"><span class="badge" style="color:var(--accent)">sample</span></c:when><c:otherwise><span class="badge muted">hidden</span></c:otherwise></c:choose></strong>
        <form method="post" action="${ctx}/studio/testcases/${tc.id}/delete" onsubmit="return confirm('Delete this test case?')">
          <t:csrf/><button class="btn btn-sm btn-danger" type="submit">Delete</button>
        </form>
      </div>
      <div class="sample-grid">
        <div><div class="lbl small muted">Input</div><pre style="max-height:160px"><c:out value="${tc.input}"/><c:if test="${fn:length(tc.input) >= 2000}">…</c:if></pre></div>
        <div><div class="lbl small muted">Expected output</div><pre style="max-height:160px"><c:out value="${tc.expectedOutput}"/><c:if test="${fn:length(tc.expectedOutput) >= 2000}">…</c:if></pre></div>
      </div>
    </div>
  </c:forEach>

  <form method="post" action="${ctx}/studio/exercises/${problem.id}/testcases" class="card">
    <t:csrf/>
    <h3>Add a test case</h3>
    <div class="sample-grid">
      <div class="field"><label for="tc-input">Input</label><textarea id="tc-input" name="input" rows="5" class="mono"></textarea></div>
      <div class="field"><label for="tc-output">Expected output</label><textarea id="tc-output" name="expected_output" rows="5" class="mono" required></textarea></div>
    </div>
    <div class="field"><label class="check"><input type="checkbox" name="is_sample" value="1"> Show as a sample on the problem page</label></div>
    <button class="btn btn-primary" type="submit">Add test case</button>
  </form>
</c:if>
</t:layout>
