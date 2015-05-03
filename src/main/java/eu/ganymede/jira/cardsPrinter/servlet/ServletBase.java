package eu.ganymede.jira.cardsPrinter.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.query.Query;

public abstract class ServletBase extends HttpServlet{
    protected UserManager userManager;
    protected LoginUriProvider loginUriProvider;
    protected JiraAuthenticationContext jiraAuthenticationContext;
    protected SearchService searchService;
    protected CustomField storyPointsField;
    protected CustomFieldManager customFieldManager;
    protected TemplateRenderer templateRenderer;
    
    protected String baseUrl;
    protected String jqlQuery;
    protected String jqlQueryUrl;
    protected Query parsedQuery;
    Map<String, Object> context = new HashMap<String, Object>();
    
    public void setUserManager (UserManager userManager) { this.userManager = userManager; }
    public void setLoginUriProvider (LoginUriProvider loginUriProvider) { this.loginUriProvider = loginUriProvider; }
    public void setJiraAuthenticationContext (JiraAuthenticationContext jiraAuthenticationContext) { this.jiraAuthenticationContext = jiraAuthenticationContext; }
    public void setSearchService (SearchService searchService) { this.searchService = searchService; }
    public void setCustomFieldManager (CustomFieldManager customFieldManager) { 
	this.customFieldManager = customFieldManager; 
	this.storyPointsField = customFieldManager.getCustomFieldObjectByName("Story Points");
    }
    public void setTemplateRenderer (TemplateRenderer templateRenderer) { this.templateRenderer = templateRenderer; }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	String username = userManager.getRemoteUsername(request);
	
	String jqlQuery = request.getParameter("jqlQuery");
	String jqlQueryUrl = response.encodeURL(jqlQuery);
	
	if (username == null)
	{
	    redirectToLogin(request, response);
	    return;
	}
		
	baseUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl");
	
	ApplicationUser applicationUser = this.jiraAuthenticationContext.getUser();
	User user = ApplicationUsers.toDirectoryUser(applicationUser);
	
	if (jqlQuery != null) {
	    ParseResult parseResult = this.searchService.parseQuery(user, jqlQuery);
	    
	    if (false == parseResult.isValid())
	    {
		StringBuilder responseBuilder = new StringBuilder();
		responseBuilder.append("Invalid JQL query:<br/><ul><li>Errors:<ul>");
		for(String error: parseResult.getErrors().getErrorMessages()) {
		    responseBuilder.append("<li>").append(error).append("</li>");		
		}
		
		responseBuilder.append("</ul></li><li>Warnings:<ul>");
		for(String warning: parseResult.getErrors().getWarningMessages()) {
		    responseBuilder.append("<li>").append(warning).append("</li>");		
		}
		
		responseBuilder.append("</ul></li></ul>");
		
		
		context.put("errorMessageHtml", responseBuilder.toString());
	
		templateRenderer.render("templates/index.vm", context, response.getWriter());
		return;
	    }
	    parsedQuery = parseResult.getQuery();
	}
	context.put("jqlQuery", jqlQuery);
	context.put("jqlQueryUrl", jqlQueryUrl);
	
	try {
	    processRequest(request, response, user);
	}
	catch(Exception e) {
	    StringBuilder responseBuilder = new StringBuilder();
	    responseBuilder.append("Unhandled exception occured:")
		.append("</br>")
		.append(e.getMessage())
		.append("</br>")
		.append(e.getStackTrace());
	    
	    context.put("errorMessageHtml", responseBuilder.toString());
    
	    templateRenderer.render("templates/index.vm", context, response.getWriter());
	}
    }
    
    	
    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
	response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }
    
    private URI getUri(HttpServletRequest request)
    {
	StringBuffer builder = request.getRequestURL();
	if (request.getQueryString() != null)
	{
	    builder.append("?");
	    builder.append(request.getQueryString());
	}
	return URI.create(builder.toString());
    } 
    
    protected abstract void processRequest(HttpServletRequest request, HttpServletResponse response, User user) throws IOException, SearchException;
    
}