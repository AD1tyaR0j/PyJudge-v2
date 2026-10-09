<%-- Floating-label input for the login and sign-up forms. "i" staggers its entrance animation. --%>
<%@ tag pageEncoding="UTF-8" body-content="empty" trimDirectiveWhitespaces="true" dynamic-attributes="extra" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="type" %>
<%@ attribute name="ico" %>
<%@ attribute name="value" %>
<%@ attribute name="i" %>
<%@ attribute name="password" type="java.lang.Boolean" %>
<div class="ff" style="--i: ${empty i ? 0 : i}">
  <t:icon name="${empty ico ? 'user' : ico}"/>
  <input id="${name}" name="${name}" type="${empty type ? 'text' : type}" value="${fn:escapeXml(value)}" placeholder=" "
    <c:forEach var="attr" items="${extra}"> ${attr.key}="${fn:escapeXml(attr.value)}"</c:forEach>>
  <label for="${name}"><c:out value="${label}"/></label>
  <c:if test="${password}">
    <button class="pw-toggle" type="button" aria-label="Show password" aria-controls="${name}">
      <span class="show"><t:icon name="eye"/></span><span class="hide"><t:icon name="eye-off"/></span>
    </button>
  </c:if>
</div>
