<%-- Newer / older links under a paged list. "base" is the URL up to and including "page=". --%>
<%@ tag pageEncoding="UTF-8" body-content="empty" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="page" required="true" type="com.pyjudge.model.Page" %>
<%@ attribute name="base" required="true" %>
<c:if test="${page.pages > 1}">
  <div class="pagination">
    <c:if test="${page.hasPrev}"><a class="btn btn-sm" href="${base}${page.prevNum}">← Newer</a></c:if>
    <span class="muted small" style="align-self:center">Page ${page.page} of ${page.pages}</span>
    <c:if test="${page.hasNext}"><a class="btn btn-sm" href="${base}${page.nextNum}">Older →</a></c:if>
  </div>
</c:if>
