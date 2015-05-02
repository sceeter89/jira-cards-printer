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
import java.util.ArrayList;

import com.atlassian.templaterenderer.TemplateRenderer;

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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.exceptions.COSVisitorException;

public class CardsPrintPreviewServlet extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(CardsPrintPreviewServlet.class);
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final SearchService searchService;
    private final CustomField storyPointsField;
    private final TemplateRenderer templateRenderer;
    
    private final PDFont font = PDType1Font.HELVETICA;
    private final float cardWidth = cmToUnit(6.5f);
    private final float cardHeight = cmToUnit(4f);
    private final int cardsInRow = 3;
    private final int cardsInColumn = 6;
    
    public CardsPrintPreviewServlet(
	JiraAuthenticationContext jiraAuthenticationContext,
	SearchService searchService,
	CustomFieldManager customFieldManager,
	TemplateRenderer templateRenderer) {
	
	this.jiraAuthenticationContext = jiraAuthenticationContext;
	this.searchService = searchService;
	this.storyPointsField = customFieldManager.getCustomFieldObjectByName("Story Points");
	this.templateRenderer = templateRenderer;
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
	String jqlQuery = req.getParameter("jqlQuery");
	StringBuilder responseBuilder = new StringBuilder();
	Map<String, Object> context = new HashMap<String, Object>();     
        
        if (jqlQuery != null) {
	    ApplicationUser applicationUser = this.jiraAuthenticationContext.getUser();
	    User user = ApplicationUsers.toDirectoryUser(applicationUser);
	    
	    SearchService.ParseResult parseResult = this.searchService.parseQuery(user, jqlQuery);
	    
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
		    try {
			ByteArrayOutputStream outputPdfBytes = generatePdfCardsPreview(issueCards);	
			resp.setContentLength(outputPdfBytes.size());
			ServletOutputStream sos;
			sos = resp.getOutputStream();
			outputPdfBytes.writeTo(sos);
			sos.flush();
			return;
		    }
		    catch(COSVisitorException e) {
			return;
		    }
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
        
        if (responseBuilder.length() > 0)
	    context.put("errorMessage", responseBuilder.toString());
        
        templateRenderer.render("templates/index.vm", context, resp.getWriter());
        
    }
    
    private float cmToUnit(float centimeters) {
	return centimeters * 10 / 0.35278f /*1/72 inch*/;
    }
    
    private ByteArrayOutputStream generatePdfCardsPreview(List<CardInformation> cards) throws IOException, COSVisitorException {
	PDDocument doc = new PDDocument();
	PDPage page = new PDPage();
	doc.addPage(page);
	
	PDPageContentStream contentStream = new PDPageContentStream(doc, page);	
        contentStream.setFont(font, 8);
        contentStream.setLineWidth(1);
	int cardsPerPage = cardsInColumn * cardsInRow;
	for (int i = 0; i < cards.size(); i++) {
	    int pageCardIndex = i % cardsPerPage;
	    
	    if (i > 0 && i % cardsPerPage == 0) {
		page = new PDPage();
		doc.addPage(page);
		contentStream.close();
		contentStream = new PDPageContentStream(doc, page);
		contentStream.setLineWidth(1);
	    }

	    drawSingleCard(cmToUnit(1) + (pageCardIndex % cardsInRow) * cardWidth,
			   cmToUnit(1) + (pageCardIndex / cardsInRow) * cardHeight,
			   cards.get(i), contentStream);
	}
	
	contentStream.close();
	
	ByteArrayOutputStream baosPDF = new ByteArrayOutputStream();
	doc.save(baosPDF);
	doc.close();
	
	return baosPDF;
    }
    
    private void drawSingleCard(float left, float top, CardInformation card, PDPageContentStream contentStream) throws IOException {
	contentStream.setStrokingColor(0, 0, 0);
	contentStream.setNonStrokingColor(0, 0, 0);
	
	drawRectangle(left, top, cardWidth, cardHeight, contentStream);
	
	//contentStream.setStrokingColor(255, 0, 0);
	drawHorizontalLine(left, top + cardHeight - cmToUnit(1.2f), cardWidth, contentStream);
	
	//contentStream.setStrokingColor(0, 255, 0);
	contentStream.drawLine(left, top + cmToUnit(1f), left + cardWidth, top + cmToUnit(1));
	
	//contentStream.setNonStrokingColor(0, 0, 255);
	drawStringToLeft(left + cmToUnit(0.1f), top + cardHeight - cmToUnit(0.29f), 10, card.getKey(), contentStream);
	drawStringToLeft(left + cmToUnit(3.0f), top + cardHeight - cmToUnit(0.5f), 6, "Subtasks: " + Integer.toString(card.getSubtasks()), contentStream);
	drawStringToLeft(left + cmToUnit(0.1f), top + cardHeight - cmToUnit(1), 8, card.getSummary(), contentStream);
	drawStringToLeft(left + cmToUnit(0.1f), top + cmToUnit(0.25f), 8, "Story points: " + Integer.toString(card.getStoryPoints()), contentStream);
    }
    
    private void drawStringToLeft(float left, float top, float fontSize, String text, PDPageContentStream contentStream) throws IOException {
	contentStream.setFont(font, fontSize);
	contentStream.beginText();
        contentStream.moveTextPositionByAmount(left + cmToUnit(0.2f), top - cmToUnit(0.1f));
        contentStream.drawString(text);
	contentStream.endText();
    }
    
    private void drawStringWithWordWrap(float left, float top, float fontSize, String text, PDPageContentStream contentStream, float maxWidth) throws IOException {
	StringBuilder currentLine = new StringBuilder();
	
    }
    
    private void drawHorizontalLine(float left, float top, float width, PDPageContentStream contentStream) throws IOException {
	contentStream.drawLine(left, top + cmToUnit(0.5f), left + cardWidth, top + cmToUnit(0.5f));
    }
    
    private void drawRectangle(float left, float top, float width, float height, PDPageContentStream contentStream) throws IOException {
	contentStream.drawLine(left, top, left + width, top);
	contentStream.drawLine(left + width, top, left + width, top + height);
	contentStream.drawLine(left + width, top + height, left, top + height);
	contentStream.drawLine(left, top + height, left, top);
    }
}