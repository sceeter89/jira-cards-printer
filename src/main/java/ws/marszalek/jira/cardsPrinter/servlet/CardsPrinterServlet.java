package ws.marszalek.jira.cardsPrinter.servlet;

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
import java.util.ArrayList;

import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.issue.Issue;
import com.atlassian.crowd.embedded.api.User;

import ws.marszalek.jira.cardsPrinter.CardInformation;

public class CardsPrinterServlet extends ServletBase {
    
    @Override
    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException, SearchException
    {		
	StringBuilder responseBuilder = new StringBuilder();
	resp.setContentType("text/html");
        
        if (parsedQuery != null) {	
	    SearchResults result = this.searchService.search(user, parsedQuery, PagerFilter.getUnlimitedFilter());
	
	    List<Issue> issues = result.getIssues();
	    List<CardInformation> issueCards = new ArrayList<CardInformation>();
	    
	    for(Issue issue: issues) {
		issueCards.add(issueToCardInfo(issue));
	    }
	    context.put("issues", issueCards);
    
        }
        
        templateRenderer.render("templates/index.vm", context, resp.getWriter());
    }
}