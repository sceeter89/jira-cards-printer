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

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.crowd.embedded.api.User;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;

public class CardsPrintPreviewServlet extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(CardsPrintPreviewServlet.class);
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final SearchService searchService;
    private final CustomField storyPointsField;
    
    public CardsPrintPreviewServlet(
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
	    .append("<title>Print CardsPrintPreviewServlet</title>")
	    .append("<style>")
	    .append("table.card")
	    .append("</style>")
	    .append("</head>")
	    .append("<body>");
	        
        if (jqlQuery != null) {
	    ApplicationUser applicationUser = this.jiraAuthenticationContext.getUser();
	    User user = ApplicationUsers.toDirectoryUser(applicationUser);
	    
	    SearchService.ParseResult parseResult = this.searchService.parseQuery(user, jqlQuery);
	    
	    Map<String, Object> context = new HashMap<String, Object>();
	    if (false == parseResult.isValid()) {
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
		    responseBuilder.append("<div>");
		    for(Issue issue: issues) {
			String key = issue.getKey();
			String summary = issue.getSummary();
			int subtasks = issue.getSubTaskObjects().size();
			int storyPoints = this.storyPointsField != null && this.storyPointsField.getValue(issue) != null ?
					    Math.round((Float)this.storyPointsField.getValue(issue))
					    : -1;
			
			responseBuilder.append("<div>")
			    .append("<table class=\"card\">")
			    .append("<tr><td>").append(key).append("</td></tr>")
			    .append("<tr><td>").append(summary).append("</td></tr>")
			    .append("<tr><td>").append(storyPoints).append("</td></tr>")
			    .append("<tr><td>").append(subtasks).append("</td></tr>")
			    .append("</table>")
			    .append("</div>");
		    }
		    responseBuilder.append("</div>");
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