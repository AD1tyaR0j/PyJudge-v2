<t:layout title="Content approval">
<p class="small"><a href="${ctx}/admin">← Admin dashboard</a></p>
<div class="page-head"><div><h1>Content approval</h1><p class="muted">Review exercises and lessons submitted by instructors. The author gets a message with your decision.</p></div></div>

<h2>Exercises (${fn:length(pending)})</h2>
<c:forEach var="p" items="${pending}">
  <c:set var="testCount" value="${empty tests[p.id] ? 0 : tests[p.id]}"/>
  <div class="card" style="margin-bottom:16px">
    <div class="card-head">
      <div>
        <h3><c:out value="${p.title}"/> <t:difficulty d="${p.difficulty}"/></h3>
        <div class="muted small">by <c:out value="${empty p.author ? 'deleted user' : p.author.displayName}"/> · submitted ${pj:dt(p.updatedAt)} UTC
          · ⏱ ${p.timeLimit} s · 💾 ${p.memoryLimit} MB</div>
        <div style="margin-top:4px"><t:ptags problem="${p}"/></div>
      </div>
      <a class="btn btn-sm" href="${ctx}/problems/${p.slug}" target="_blank" rel="noopener">Preview &amp; try it ↗</a>
    </div>
    <details>
      <summary class="small muted" style="cursor:pointer">Statement</summary>
      <div class="description">${pj:md(p.description)}</div>
    </details>
    <p class="small">
      <c:choose>
        <c:when test="${testCount > 0}">${samples[p.id]} sample + ${testCount - samples[p.id]} hidden test cases</c:when>
        <c:otherwise><span style="color:var(--red)">⚠ No test cases yet. It can't be approved until the author adds some.</span></c:otherwise>
      </c:choose>
    </p>
    <form method="post" action="${ctx}/admin/exercises/${p.id}/review">
      <t:csrf/>
      <div class="field"><label for="note-${p.id}">Note to the instructor <span class="muted small">(required when rejecting)</span></label>
        <textarea id="note-${p.id}" name="note" rows="2" placeholder="e.g. Please add an edge case for empty input."></textarea></div>
      <div class="actions">
        <button class="btn btn-success" name="decision" value="approve" type="submit" ${testCount == 0 ? 'disabled' : ''}>✔ Approve &amp; publish</button>
        <button class="btn btn-danger" name="decision" value="reject" type="submit">✖ Reject</button>
      </div>
    </form>
  </div>
</c:forEach>
<c:if test="${empty pending}">
  <div class="card empty">🎉 All caught up. No exercises are waiting for review.</div>
</c:if>

<h2 class="section">Lessons (${fn:length(pendingLessons)})</h2>
<c:forEach var="l" items="${pendingLessons}">
  <div class="card" style="margin-bottom:16px">
    <div class="card-head">
      <div>
        <h3><c:out value="${l.topic.icon}"/> <c:out value="${l.title}"/></h3>
        <div class="muted small"><c:out value="${l.topic.title}"/> · by <c:out value="${empty l.author ? 'deleted user' : l.author.displayName}"/> · submitted ${pj:dt(l.updatedAt)} UTC
          · ${l.minutes} min · ${l.questionCount} quiz question${l.questionCount == 1 ? '' : 's'}</div>
      </div>
      <a class="btn btn-sm" href="${ctx}/learn/${l.topic.slug}/${l.slug}" target="_blank" rel="noopener">Preview lesson ↗</a>
    </div>
    <form method="post" action="${ctx}/admin/lessons/${l.id}/review">
      <t:csrf/>
      <div class="field"><label for="lnote-${l.id}">Note to the instructor <span class="muted small">(required when rejecting)</span></label>
        <textarea id="lnote-${l.id}" name="note" rows="2" placeholder="e.g. Add a runnable example for slicing."></textarea></div>
      <div class="actions">
        <button class="btn btn-success" name="decision" value="approve" type="submit">✔ Approve &amp; publish</button>
        <button class="btn btn-danger" name="decision" value="reject" type="submit">✖ Reject</button>
      </div>
    </form>
  </div>
</c:forEach>
<c:if test="${empty pendingLessons}">
  <div class="card empty">No lessons are waiting for review.</div>
</c:if>
</t:layout>
