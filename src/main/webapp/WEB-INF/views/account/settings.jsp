<t:layout title="Account settings">
<div class="page-head">
  <div class="row">
    <span class="avatar lg"><c:out value="${currentUser.initial}"/></span>
    <div><h1>Account settings</h1><span class="muted">@<c:out value="${currentUser.username}"/></span> <t:role r="${currentUser.role}"/></div>
  </div>
  <a class="btn" href="${ctx}/u/${fn:escapeXml(currentUser.username)}">View public profile</a>
</div>

<div class="grid grid-2">
  <form method="post" class="card">
    <t:csrf/><input type="hidden" name="action" value="profile">
    <h3>Profile</h3>
    <div class="field"><label for="name">Full name</label><input id="name" name="name" type="text" value="${fn:escapeXml(profile.name)}" required maxlength="100"></div>
    <div class="field"><label for="email">Email</label><input id="email" name="email" type="email" value="${fn:escapeXml(profile.email)}" required></div>
    <div class="field"><label for="bio">About you <span class="muted small">(shown on your profile)</span></label>
      <textarea id="bio" name="bio" rows="4" maxlength="1000" placeholder="${currentUser.author ? 'Your teaching background, office hours…' : 'What are you learning Python for?'}"><c:out value="${profile.bio}"/></textarea></div>
    <button class="btn btn-primary" type="submit">Save profile</button>
  </form>
  <form method="post" class="card">
    <t:csrf/><input type="hidden" name="action" value="password">
    <h3>Change password</h3>
    <div class="field"><label for="current_password">Current password</label><input id="current_password" name="current_password" type="password" required autocomplete="current-password"></div>
    <div class="field"><label for="new_password">New password</label><input id="new_password" name="new_password" type="password" required minlength="8" autocomplete="new-password"></div>
    <div class="field"><label for="confirm_password">Confirm new password</label><input id="confirm_password" name="confirm_password" type="password" required minlength="8" autocomplete="new-password"></div>
    <button class="btn" type="submit">Change password</button>
  </form>
</div>
</t:layout>
