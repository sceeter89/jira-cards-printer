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

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import eu.ganymede.jira.cardsPrinter.CardInformation;

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
	        	
	SearchResults result = null;
	try {
	    result = this.searchService.search(user, parseResult.getQuery(), PagerFilter.getUnlimitedFilter());
	
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
	    
	    resp.setContentType("application/pdf");
	    resp.setHeader("Content-disposition",
		    "inline; filename=AgileCards.pdf" );
	    
	    ByteArrayOutputStream outputPdfBytes = generatePdfCardsPreview(issueCards);	
	    resp.setContentLength(outputPdfBytes.size());
	    ServletOutputStream sos;
	    sos = resp.getOutputStream();
	    baos.writeTo(sos);
	    sos.flush();
	    return
	}
	catch(SearchException e) {
	    responseBuilder.append("Searching for issues was insterrupted by error:")
		.append("</br>")
		.append(e.getMessage())
		.append("</br>")
		.append(e.getStackTrace());
	}
	    
        if (responseBuilder.length() > 0)
	    context.put("errorMessage", responseBuilder.toString());
	
	resp.setContentType("text/html");
        templateRenderer.render("templates/index.vm", context, resp.getWriter());
    }
    
    private ByteArrayOutputStream generatePdfCardsPreview(List<CardInformation> cards) {
	//Document doc = new Document();
	ByteArrayOutputStream baosPDF = new ByteArrayOutputStream();
	PdfWriter docWriter = null;
	docWriter = PdfWriter.getInstance(doc, baosPDF);
    }
}