<t:authLayout title="Log in" cardTitle="Welcome back" cardSub="Log in to keep coding."
              showcaseText="Your exercises, streak, challenges, and instructor feedback are waiting for you.">
  <jsp:attribute name="showcaseTitle">Pick up right<br><span class="grad-text">where you left off.</span></jsp:attribute>
  <jsp:attribute name="footer">
    <c:choose>
      <c:when test="${site.registration_open}">New here? <a href="${ctx}/register">Create a student account →</a></c:when>
      <c:otherwise>Need an account? Ask an administrator.</c:otherwise>
    </c:choose>
  </jsp:attribute>
  <jsp:body>
<%-- No action attribute: the form posts back to this URL, keeping its ?next= parameter. --%>
<form method="post" class="auth-form">
  <t:csrf/>
  <t:authField name="identifier" label="Username or email" ico="user" value="${identifier}" i="3"
               autocomplete="username" required="required" autofocus="autofocus"/>
  <t:authField name="password" label="Password" type="password" ico="lock" i="4"
               autocomplete="current-password" required="required" password="true"/>
  <div class="caps-hint" role="status">⇪ Caps Lock is on</div>
  <div class="auth-row" style="--i:5">
    <label class="fancy-check"><input type="checkbox" name="remember" value="1"><span class="box"></span> Remember me</label>
  </div>
  <button class="btn-auth" type="submit" style="--i:6">
    <span class="label">Log in</span><span class="spinner-sm" aria-hidden="true"></span>
  </button>
</form>
  </jsp:body>
</t:authLayout>
