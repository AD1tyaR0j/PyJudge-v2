<%-- An instructor's 0-100 grade as a coloured pill, or a dash when there is none. --%>
<%@ tag pageEncoding="UTF-8" body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="g" type="java.lang.Integer" %>
<c:choose>
  <c:when test="${g == null}"><span class="muted">—</span></c:when>
  <c:otherwise><span class="grade-pill ${g >= 80 ? 'grade-hi' : g >= 50 ? 'grade-mid' : 'grade-lo'}">${g}</span></c:otherwise>
</c:choose>
