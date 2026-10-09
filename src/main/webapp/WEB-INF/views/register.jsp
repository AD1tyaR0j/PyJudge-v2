<t:authLayout title="Sign up" cardTitle="Create your account"
              cardSub="Student accounts are free. Instructor accounts are created by an administrator."
              showcaseText="Solve hands-on exercises, get instant verdicts, join challenges, and get feedback from instructors.">
  <jsp:attribute name="showcaseTitle">Learn Python by<br><span class="grad-text">actually writing it.</span></jsp:attribute>
  <jsp:attribute name="footer">Already have an account? <a href="${ctx}/login">Log in →</a></jsp:attribute>
  <jsp:body>
<form method="post" class="auth-form">
  <t:csrf/>
  <t:authField name="name" label="Full name" ico="badge" value="${form.name}" i="3" autocomplete="name"
               required="required" maxlength="100" autofocus="autofocus"/>
  <t:authField name="username" label="Username" ico="user" value="${form.username}" i="4" autocomplete="username"
               required="required" minlength="3" maxlength="30" pattern="[A-Za-z0-9_]+"/>
  <t:authField name="email" label="Email" type="email" ico="mail" value="${form.email}" i="5" autocomplete="email"
               required="required"/>
  <t:authField name="password" label="Password (8+ characters)" type="password" ico="lock" i="6"
               autocomplete="new-password" required="required" minlength="8" password="true"/>
  <div class="strength" style="--i:6" aria-live="polite">
    <div class="strength-bars"><span></span><span></span><span></span><span></span></div>
    <span class="strength-label">Use 8+ characters with a mix of letters, numbers, and symbols</span>
  </div>
  <t:authField name="confirm" label="Confirm password" type="password" ico="lock" i="7"
               autocomplete="new-password" required="required" minlength="8" password="true"/>
  <div class="caps-hint" role="status">⇪ Caps Lock is on</div>
  <div class="match-hint" aria-live="polite"></div>
  <button class="btn-auth" type="submit" style="--i:8">
    <span class="label">Create account</span><span class="spinner-sm" aria-hidden="true"></span>
  </button>
</form>
  </jsp:body>
</t:authLayout>
