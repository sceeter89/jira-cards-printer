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

import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.issue.Issue;

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import eu.ganymede.jira.cardsPrinter.CardInformation;
import eu.ganymede.jira.cardsPrinter.PdfCardsPrinter;

public class CardsPrintPreviewServlet extends ServletBase {    
    @Override
    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException, SearchException {
	StringBuilder responseBuilder = new StringBuilder();
        
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
	    resp.setContentType("application/pdf");
	    resp.setHeader("Content-disposition",
		    "inline; filename=AgileCards.pdf" );
	    
	    PdfCardsPrinter printer = new PdfCardsPrinter();
	    ByteArrayOutputStream outputPdfBytes = printer.generatePdfCardsPreview(issueCards);	
	    resp.setContentLength(outputPdfBytes.size());
	    ServletOutputStream sos;
	    sos = resp.getOutputStream();
	    outputPdfBytes.writeTo(sos);
	    sos.flush();
	    return;
	    
        }
        
        templateRenderer.render("templates/index.vm", context, resp.getWriter());        
    }
}