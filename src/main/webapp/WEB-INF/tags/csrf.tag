<%-- Hidden field every POST form needs; CsrfFilter compares it with the token in the session. --%>
<%@ tag pageEncoding="UTF-8" body-content="empty" trimDirectiveWhitespaces="true" %>
<input type="hidden" name="csrf_token" value="${csrfToken}">
