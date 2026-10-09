<%-- The topic tags of an exercise. --%>
<%@ tag pageEncoding="UTF-8" body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="problem" required="true" type="com.pyjudge.model.Problem" %>
<c:forEach var="tag" items="${problem.tagList}"><span class="tag"><c:out value="${tag}"/></span></c:forEach>
