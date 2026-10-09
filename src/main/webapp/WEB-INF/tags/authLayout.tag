<%-- The two-column login / sign-up screen: an animated showcase on the left, the form card on the right. --%>
<%@ tag pageEncoding="UTF-8" body-content="scriptless" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="cardTitle" required="true" %>
<%@ attribute name="cardSub" required="true" %>
<%@ attribute name="showcaseText" required="true" %>
<%@ attribute name="showcaseTitle" fragment="true" %>
<%@ attribute name="footer" fragment="true" %>
<c:set var="hasError" value="false"/>
<c:forEach var="f" items="${flashes}"><c:if test="${f[0] == 'error'}"><c:set var="hasError" value="true"/></c:if></c:forEach>
<t:layout title="${title}" bodyClass="auth" mainClass="auth-main" hideFlashes="true">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/static/css/auth.css"></jsp:attribute>
  <jsp:attribute name="scripts"><script src="${ctx}/static/js/auth.js"></script></jsp:attribute>
  <jsp:body>
<div class="auth-shell no-reveal">
  <section class="auth-showcase" aria-hidden="true">
    <div class="eyebrow"><span class="pulse-dot"></span> Python practice, judged in seconds</div>
    <h1 class="auth-headline"><jsp:invoke fragment="showcaseTitle"/></h1>
    <p class="auth-lede"><c:out value="${showcaseText}"/></p>

    <div class="code-window" id="code-window">
      <div class="cw-bar">
        <span class="dot"></span><span class="dot"></span><span class="dot"></span>
        <span class="cw-file">two_sum.py</span>
        <span class="cw-run">▶ Submit</span>
      </div>
      <pre class="cw-code"><span class="cw-line"><span class="k">def</span> <span class="f">two_sum</span>(nums, target):</span>
<span class="cw-line">    seen = {}</span>
<span class="cw-line">    <span class="k">for</span> i, x <span class="k">in</span> <span class="f">enumerate</span>(nums):</span>
<span class="cw-line">        <span class="k">if</span> target - x <span class="k">in</span> seen:</span>
<span class="cw-line">            <span class="k">return</span> seen[target - x], i</span>
<span class="cw-line">        seen[x] = i</span></pre>
      <div class="cw-result">
        <div class="cw-judging"><span class="cw-spin"></span> Judging against 6 test cases…</div>
        <div class="cw-verdict"><span class="cw-badge">✔ Accepted</span><span>6/6 tests · 48 ms</span></div>
      </div>
    </div>

    <div class="chips">
      <span class="chip" style="--i:0">🏁 Weekly challenges</span>
      <span class="chip" style="--i:1">📝 Instructor feedback</span>
      <span class="chip" style="--i:2">📈 Progress tracking</span>
    </div>
  </section>

  <section class="auth-panel">
    <div class="auth-card ${hasError ? 'shake-in' : ''}">
      <div class="auth-logo" style="--i:0">🐍</div>
      <h2 class="auth-card-title" style="--i:1"><c:out value="${cardTitle}"/></h2>
      <p class="auth-card-sub" style="--i:2"><c:out value="${cardSub}"/></p>
      <c:forEach var="f" items="${flashes}">
        <div class="flash flash-${f[0]}" role="${f[0] == 'error' ? 'alert' : 'status'}"><c:out value="${f[1]}"/></div>
      </c:forEach>
      <jsp:doBody/>
      <div class="auth-foot"><jsp:invoke fragment="footer"/></div>
    </div>
  </section>
</div>
  </jsp:body>
</t:layout>
