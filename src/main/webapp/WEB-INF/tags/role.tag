<%@ tag pageEncoding="UTF-8" body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="r" required="true" type="com.pyjudge.model.Role" %>
<span class="role-badge role-${r.key}">${r.label}</span>
