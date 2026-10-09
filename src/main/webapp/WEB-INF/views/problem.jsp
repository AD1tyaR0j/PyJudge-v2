<t:layout title="${problem.title}" bodyClass="workspace" mainClass="" flashClass="container">
  <jsp:attribute name="scripts">
<script>window.PROBLEM = ${pj:json(problemJs)};</script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.45.0/min/vs/loader.min.js"></script>
<script src="${ctx}/static/js/editor.js"></script>
  </jsp:attribute>
  <jsp:body>
<div class="split">
  <section class="pane statement">
    <div class="row" style="margin-bottom:6px">
      <a href="${ctx}/problems" class="small">← All exercises</a>
      <c:if test="${problem.editableBy(currentUser)}">
        <a href="${ctx}/studio/exercises/${problem.id}/edit" class="small">Edit</a>
      </c:if>
    </div>
    <c:if test="${not problem.approved}">
      <div class="note ${problem.status.key == 'rejected' ? 'bad' : ''}">👁 Preview only: this exercise is <strong>${problem.status.key}</strong> and hidden from students.</div>
    </c:if>
    <h1>${problem.id}. <c:out value="${problem.title}"/></h1>
    <div class="row small muted" style="margin-bottom:8px">
      <t:difficulty d="${problem.difficulty}"/>
      <span>⏱ ${problem.timeLimit} s</span>
      <span>💾 ${problem.memoryLimit} MB</span>
    </div>
    <div><t:ptags problem="${problem}"/></div>

    <div class="description">${pj:md(problem.description)}</div>

    <c:if test="${not empty samples}">
      <h3>Examples</h3>
      <c:forEach var="tc" items="${samples}" varStatus="row">
        <div class="sample">
          <div class="row spread small"><strong>Example ${row.count}</strong>
            <button class="btn btn-sm" type="button" data-use-input="${row.index}">Use as input</button>
          </div>
          <div class="sample-grid" style="margin-top:6px">
            <div><div class="lbl">Input</div><pre class="sample-in"><c:out value="${tc.input}"/></pre></div>
            <div><div class="lbl">Output</div><pre><c:out value="${tc.expectedOutput}"/></pre></div>
          </div>
        </div>
      </c:forEach>
    </c:if>

    <c:if test="${not empty last}">
      <p class="small muted">Your last submission:
        <a href="${ctx}/submissions/${last.id}"><t:verdict v="${last.verdict}"/></a>
        · ${pj:dt(last.submittedAt)} UTC
      </p>
    </c:if>
  </section>

  <section class="ide">
    <div class="toolbar">
      <div class="row small muted"><strong style="color:var(--text)">Python 3</strong>
        <button class="btn btn-sm" id="reset-btn" type="button" title="Restore the starter code">Reset</button>
      </div>
      <div class="row">
        <c:choose>
          <c:when test="${not empty currentUser}">
            <button class="btn" id="run-btn" type="button" title="Ctrl+Enter">▶ Run</button>
            <button class="btn btn-success" id="submit-btn" type="button" title="Ctrl+Shift+Enter">Submit</button>
          </c:when>
          <c:otherwise>
            <a class="btn btn-primary" href="${ctx}/login?next=${pj:url(path)}">Log in to run &amp; submit</a>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
    <div id="editor"></div>
    <div class="console">
      <div class="tabs" role="tablist">
        <button class="active" data-tab="input" type="button">Custom input</button>
        <button data-tab="output" type="button">Output</button>
        <button data-tab="result" type="button">Result</button>
        <span style="margin-left:auto; align-self:center" class="small muted"><kbd>Ctrl</kbd>+<kbd>Enter</kbd> run</span>
      </div>
      <div class="tab-panel active" data-panel="input">
        <textarea id="stdin" class="mono" spellcheck="false" placeholder="Input sent to your program's stdin"><c:if test="${not empty samples}"><c:out value="${samples[0].input}"/></c:if></textarea>
      </div>
      <div class="tab-panel" data-panel="output"><div id="run-output" class="muted">Run your code to see its output here.</div></div>
      <div class="tab-panel" data-panel="result"><div id="submit-result" class="muted">Submit to judge your code against all test cases.</div></div>
    </div>
  </section>
</div>
  </jsp:body>
</t:layout>
