<%@ tag pageEncoding="UTF-8" body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ attribute name="v" required="true" type="com.pyjudge.model.Verdict" %>
<span class="badge v-${v.key}">${v.label}</span>
