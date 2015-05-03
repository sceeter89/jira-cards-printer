package eu.ganymede.jira.cardsPrinter.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.atlassian.crowd.embedded.api.User;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.issue.Issue;
import eu.ganymede.jira.cardsPrinter.CardInformation;

public class CardsPrinterServlet extends ServletBase {
    
    @Override
    protected void processRequest(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException, SearchException
    {	
	Map<String, Object> context = new HashMap<String, Object>();
	context.put("jqlQuery", jqlQueryUrl);
	context.put("baseUrl", baseUrl);
	
	StringBuilder responseBuilder = new StringBuilder();
	resp.setContentType("text/html");
        
        if (parsedQuery != null) {	
	    SearchResults result = this.searchService.search(user, parsedQuery, PagerFilter.getUnlimitedFilter());
	
	    List<Issue> issues = result.getIssues();
	    List<CardInformation> issueCards = new ArrayList<CardInformation>();
	    
	    for(Issue issue: issues) {
		String key = issue.getKey();
		String summary = issue.getSummary();
		int subtasks = issue.getSubTaskObjects().size();
		int storyPoints = this.storyPointsField != null && this.storyPointsField.getValue(issue) != null ?
				    Math.round((Float)this.storyPointsField.getValue(issue))
				    : -1;
		
		issueCards.add(new CardInformation(key,
		    summary,
		    storyPoints,
		    subtasks));
	    }
	    context.put("issues", issueCards);
    
        }
        
        templateRenderer.render("templates/index.vm", context, resp.getWriter());
    }
}