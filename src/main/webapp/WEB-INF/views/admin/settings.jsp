<t:layout title="System settings">
<p class="small"><a href="${ctx}/admin">← Admin dashboard</a></p>
<div class="page-head"><div><h1>System settings</h1><p class="muted">Changes apply to the whole platform immediately.</p></div></div>

<form method="post" class="grid grid-2">
  <t:csrf/>
  <div class="card">
    <h3>General</h3>
    <c:forTokens var="key" items="site_name,announcement" delims=",">
      <c:set var="def" value="${schema[key]}"/>
      <div class="field">
        <label for="${key}">${def.label}</label>
        <input id="${key}" name="${key}" type="text" value="${fn:escapeXml(values[key])}" maxlength="500">
        <div class="muted small" style="margin-top:4px">${def.help}</div>
      </div>
    </c:forTokens>
    <h3 style="margin-top:24px">Judge limits</h3>
    <c:forTokens var="key" items="default_time_limit,default_memory_limit,max_pending_submissions" delims=",">
      <c:set var="def" value="${schema[key]}"/>
      <div class="field">
        <label for="${key}">${def.label}</label>
        <input id="${key}" name="${key}" type="number" min="${def.min}" max="${def.max}" value="${values[key]}">
        <div class="muted small" style="margin-top:4px">${def.help}</div>
      </div>
    </c:forTokens>
  </div>
  <div class="card">
    <h3>Features</h3>
    <c:forTokens var="key" items="registration_open,require_approval,messaging_enabled,leaderboard_enabled" delims=",">
      <c:set var="def" value="${schema[key]}"/>
      <div class="switch-row">
        <div><label for="${key}">${def.label}</label><div class="muted small">${def.help}</div></div>
        <label class="switch"><input id="${key}" name="${key}" type="checkbox" value="1" ${values[key] ? 'checked' : ''}><span></span></label>
      </div>
    </c:forTokens>
    <button class="btn btn-primary" type="submit" style="width:100%; margin-top:20px">Save settings</button>
  </div>
</form>
</t:layout>
