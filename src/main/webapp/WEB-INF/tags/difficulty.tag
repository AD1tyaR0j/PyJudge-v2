<%@ tag pageEncoding="UTF-8" body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="d" required="true" type="com.pyjudge.model.Difficulty" %>
<span class="badge d-${d.key}">${d.label}</span>
