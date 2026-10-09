<%-- Accessible table version of a chart: one row per label, one column per series (up to two). --%>
<%@ tag pageEncoding="UTF-8" body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="labels" required="true" type="java.util.List" %>
<%@ attribute name="name1" required="true" %>
<%@ attribute name="values1" required="true" type="java.util.List" %>
<%@ attribute name="name2" %>
<%@ attribute name="values2" type="java.util.List" %>
<%@ attribute name="suffix" %>
<details class="table-view">
  <summary>Show as table</summary>
  <div class="table-wrap"><table>
    <thead><tr><th></th><th><c:out value="${name1}"/></th><c:if test="${not empty name2}"><th><c:out value="${name2}"/></th></c:if></tr></thead>
    <tbody>
    <c:forEach var="label" items="${labels}" varStatus="row">
      <tr><td><c:out value="${label}"/></td><td>${values1[row.index]}${suffix}</td><c:if test="${not empty name2}"><td>${values2[row.index]}${suffix}</td></c:if></tr>
    </c:forEach>
    </tbody>
  </table></div>
</details>
