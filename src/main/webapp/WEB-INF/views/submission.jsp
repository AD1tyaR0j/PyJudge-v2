<t:layout title="Submission #${sub.id}">
  <jsp:attribute name="head"><c:if test="${sub.pending}"><meta http-equiv="refresh" content="2"></c:if></jsp:attribute>
  <jsp:body>
<p class="small">
  <c:choose>
    <c:when test="${isOwner}"><a href="${ctx}/submissions">← My submissions</a></c:when>
    <c:otherwise><a href="${ctx}/instructor/submissions">← Student submissions</a></c:otherwise>
  </c:choose>
</p>
<div class="page-head">
  <h1>Submission #${sub.id}</h1>
  <c:choose>
    <c:when test="${isOwner and not canGrade and site.messaging_enabled}">
      <a class="btn" href="${ctx}/messages/new?submission=${sub.id}&amp;to=${pj:url(askUsername)}">💬 Ask an instructor about this</a>
    </c:when>
    <c:when test="${canGrade and not isOwner}">
      <a class="btn" href="${ctx}/messages/${fn:escapeXml(sub.user.username)}">💬 Message <c:out value="${sub.user.displayName}"/></a>
    </c:when>
  </c:choose>
</div>
<div class="grid grid-4" style="margin-bottom:20px">
  <div class="card"><div class="muted small">Exercise</div>
    <a href="${ctx}/problems/${sub.problem.slug}"><strong><c:out value="${sub.problem.title}"/></strong></a>
    <div style="margin-top:4px"><t:difficulty d="${sub.problem.difficulty}"/></div></div>
  <div class="card"><div class="muted small">Verdict</div><div style="margin-top:6px"><t:verdict v="${sub.verdict}"/></div>
    <div class="small muted" style="margin-top:4px">${sub.passed}/${sub.total} tests ·
      <c:choose><c:when test="${sub.runtimeMs == null}">—</c:when><c:otherwise>${sub.runtimeMs} ms</c:otherwise></c:choose></div></div>
  <div class="card"><div class="muted small">Instructor grade</div><div style="margin-top:6px; font-size:20px"><t:grade g="${sub.grade}"/></div>
    <c:if test="${not empty sub.grader}"><div class="small muted" style="margin-top:4px">by <c:out value="${sub.grader.displayName}"/></div></c:if></div>
  <div class="card"><div class="muted small">Student</div>
    <a href="${ctx}/u/${fn:escapeXml(sub.user.username)}"><strong><c:out value="${sub.user.displayName}"/></strong></a>
    <div class="small muted" style="margin-top:4px">${pj:dt(sub.submittedAt)} UTC</div></div>
</div>

<c:if test="${not empty sub.feedback}">
  <div class="note ok"><strong>Feedback<c:if test="${not empty sub.grader}"> from <c:out value="${sub.grader.displayName}"/></c:if>:</strong><br>
    <span style="white-space:pre-wrap"><c:out value="${sub.feedback}"/></span></div>
</c:if>

<c:if test="${canGrade and not sub.pending}">
  <form method="post" action="${ctx}/instructor/submissions/${sub.id}/grade" class="card section">
    <t:csrf/>
    <h3>${sub.grade == null ? 'Grade this submission' : 'Update grade'}</h3>
    <p class="muted small" style="margin-top:0">Automatic score: ${sub.scorePercent}% of tests passed. The student gets a message with your grade and feedback.</p>
    <div class="grid" style="grid-template-columns: 140px 1fr">
      <div class="field"><label for="grade">Grade (0–100)</label>
        <input id="grade" name="grade" type="number" min="0" max="100" required value="${sub.grade == null ? sub.scorePercent : sub.grade}"></div>
      <div class="field"><label for="feedback">Feedback</label>
        <textarea id="feedback" name="feedback" rows="3" placeholder="What went well, and what to improve…"><c:out value="${sub.feedback}"/></textarea></div>
    </div>
    <div class="actions">
      <button class="btn btn-primary" type="submit">Save grade</button>
      <button class="btn" type="submit" name="next" value="1">Save &amp; grade next →</button>
    </div>
  </form>
</c:if>

<c:if test="${not empty sub.error}">
  <h3>Judge details</h3>
  <pre><c:out value="${sub.error}"/></pre>
</c:if>

<h3>Code</h3>
<pre><c:out value="${sub.code}"/></pre>
  </jsp:body>
</t:layout>
