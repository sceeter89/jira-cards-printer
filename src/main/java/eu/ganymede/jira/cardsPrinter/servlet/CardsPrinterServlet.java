package eu.ganymede.jira.cardsPrinter.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;

public class CardsPrinterServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CardsPrinterServlet.class);
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final SearchService searchService;
    private final CustomField storyPointsField;
    
    public CardsPrinterServlet(
	JiraAuthenticationContext jiraAuthenticationContext,
	SearchService searchService,
	CustomFieldManager customFieldManager) {
	
	this.jiraAuthenticationContext = jiraAuthenticationContext;
	this.searchService = searchService;
	this.storyPointsField = customFieldManager.getCustomFieldObjectByName("Story Points");
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
	String jqlQuery = req.getParameter("jqlQuery");
	StringBuilder responseBuilder = new StringBuilder();
	resp.setContentType("text/html");
	
	responseBuilder.append("<html>")
	    .append("<head>")
	    .append("<meta name=\"decorator\" content=\"atl.general\">")
	    .append("<title>Print cards for your scrum/kanban board</title>")
	    .append("</head>")
	    .append("<body>");
	
	responseBuilder.append("<form method=\"GET\">")
	    .append("<label>JQL: <input type=\"text\" name=\"jqlQuery\" id=\"jqlQuery\" /></label>")
	    .append("</form>");
        
        if (jqlQuery != null) {
	    ApplicationUser applicationUser = this.jiraAuthenticationContext.getUser();
	    User user = ApplicationUsers.toDirectoryUser(applicationUser);
	    
	    SearchService.ParseResult parseResult = this.searchService.parseQuery(user, jqlQuery);
	    
	    Map<String, Object> context = new HashMap<String, Object>();
	    if (false == parseResult.isValid())
	    {
		responseBuilder.append("Invalid JQL query:<br/><ul><li>Errors:<ul>");
		for(String error: parseResult.getErrors().getErrorMessages()) {
		    responseBuilder.append("<li>").append(error).append("</li>");		
		}
		
		responseBuilder.append("</ul></li><li>Warnings:<ul>");
		for(String warning: parseResult.getErrors().getWarningMessages()) {
		    responseBuilder.append("<li>").append(warning).append("</li>");		
		}
		
		responseBuilder.append("</ul></li></ul>");
	    }
	    else {
		
		SearchResults result = null;
		try {
		    result = this.searchService.search(user, parseResult.getQuery(), PagerFilter.getUnlimitedFilter());
		
		    List<Issue> issues = result.getIssues();
		    responseBuilder.append("<table>");
		    for(Issue issue: issues) {
			String key = issue.getKey();
			String summary = issue.getSummary();
			int subtasks = issue.getSubTaskObjects().size();
			int storyPoints = this.storyPointsField != null && this.storyPointsField.getValue(issue) != null ?
					    Math.round((Float)this.storyPointsField.getValue(issue))
					    : -1;
			
			responseBuilder.append("<tr>")
			    .append("<td>").append(key).append("</td>")
			    .append("<td>").append(summary).append("</td>")
			    .append("<td>").append(storyPoints).append("</td>")
			    .append("<td>").append(subtasks).append("</td>")
			    .append("</tr>");
		    }
		    responseBuilder.append("</table>")
			.append("<br />")
			.append("<a href=\"")
			.append(ComponentAccessor.getApplicationProperties().getString("jira.baseurl"))
			.append("/plugins/servlet/ganymede/cardsprintpreview?jqlQuery=")
			.append(resp.encodeURL(jqlQuery))
			.append("\">Print</a>");
		}
		catch(SearchException e) {
		    responseBuilder.append("Searching for issues was insterrupted by error:")
			.append("</br>")
			.append(e.getMessage())
			.append("</br>")
			.append(e.getStackTrace());
		}
	    }
        }
        
	responseBuilder.append("</body>");
	responseBuilder.append("</html>");
        resp.getWriter().write(responseBuilder.toString());
        return;
    }

}