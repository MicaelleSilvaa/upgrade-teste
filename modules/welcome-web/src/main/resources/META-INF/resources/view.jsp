<%@ include file="/init.jsp" %>

<div class="welcome-portlet">
	<c:choose>
		<c:when test="<%= themeDisplay.isSignedIn() %>">
			<h2>
				<liferay-ui:message arguments="<%= HtmlUtil.escape(user.getFirstName()) %>" key="welcome.greeting" />
			</h2>

			<p><liferay-ui:message key="welcome.signed-in-text" /></p>
		</c:when>
		<c:otherwise>
			<h2><liferay-ui:message key="welcome.title" /></h2>

			<p><liferay-ui:message key="welcome.guest-text" /></p>

			<a class="btn btn-primary" href="<%= themeDisplay.getURLSignIn() %>">
				<liferay-ui:message key="sign-in" />
			</a>
		</c:otherwise>
	</c:choose>
</div>
