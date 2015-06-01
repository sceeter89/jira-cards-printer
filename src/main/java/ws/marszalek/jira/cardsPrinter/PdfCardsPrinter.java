package ws.marszalek.jira.cardsPrinter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.exceptions.COSVisitorException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class PdfCardsPrinter
{
    private final PDFont font = PDType1Font.HELVETICA;
    private final float cardWidth = cmToUnit(6.5f);
    private final float cardHeight = cmToUnit(4f);
    private final int cardsInRow = 3;
    private final int cardsInColumn = 6;
    
    public ByteArrayOutputStream generatePdfCardsPreview(List<CardInformation> cards) throws IOException
    {
	PDDocument doc = new PDDocument();
	PDPage page = new PDPage();
	doc.addPage(page);
	
	try
	{
	    PDPageContentStream contentStream = new PDPageContentStream(doc, page);	
	    contentStream.setFont(font, 8);
	    contentStream.setLineWidth(1);
	    int cardsPerPage = cardsInColumn * cardsInRow;
	    for (int i = 0; i < cards.size(); i++)
	    {
		int pageCardIndex = i % cardsPerPage;
		
		if (i > 0 && i % cardsPerPage == 0)
		{
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
	catch (COSVisitorException e)
	{
	    throw new IOException(e.getMessage());
	}
    }


    private float cmToUnit(float centimeters)
    {
	return centimeters * 10 / 0.35278f /*1/72 inch*/;
    }
    
    private void drawSingleCard(float left, float top, CardInformation card, PDPageContentStream contentStream) throws IOException
    {
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
	drawStringWithWordWrap(left + cmToUnit(0.1f), top + cardHeight - cmToUnit(1), 8, card.getSummary(), contentStream, cmToUnit(5.0f));
	drawStringToLeft(left + cmToUnit(0.1f), top + cmToUnit(0.25f), 8, "Story points: " + Integer.toString(card.getStoryPoints()), contentStream);
    }
    
    private void drawStringToLeft(float left, float top, float fontSize, String text, PDPageContentStream contentStream) throws IOException
    {
	contentStream.setFont(font, fontSize);
	contentStream.beginText();
        contentStream.moveTextPositionByAmount(left + cmToUnit(0.2f), top - cmToUnit(0.1f));
        contentStream.drawString(text);
	contentStream.endText();
    }
    
    private float getTextWidth(String string, float fontSize) throws IOException
    {
	return font.getStringWidth(string) / 1000 * fontSize;
    }
    
    private void drawStringWithWordWrap(float left, float top, float fontSize, String text, PDPageContentStream contentStream, float maxWidth) throws IOException
    {
	float currentTop = top;
	StringBuilder currentLine = new StringBuilder();
	StringBuilder inputString = new StringBuilder(text);
	
	for (int i = 0; i < inputString.length(); i++)
	{
	    char currentLetter = inputString.charAt(i);
	    
	    float currentWidth = getTextWidth(currentLine.toString() + currentLetter, fontSize);
	    if (currentWidth >= maxWidth)
	    {
		drawStringToLeft(left, currentTop, fontSize, currentLine.toString() + "-", contentStream);
		currentTop -= fontSize + 1;
		currentLine = new StringBuilder();
	    }
	    
	    currentLine.append(currentLetter);
	}
	
	if (currentLine.length() > 0)
	    drawStringToLeft(left, currentTop, fontSize, currentLine.toString(), contentStream);
    }
    
    private void drawHorizontalLine(float left, float top, float width, PDPageContentStream contentStream) throws IOException
    {
	contentStream.drawLine(left, top + cmToUnit(0.5f), left + cardWidth, top + cmToUnit(0.5f));
    }
    
    private void drawRectangle(float left, float top, float width, float height, PDPageContentStream contentStream) throws IOException
    {
	contentStream.drawLine(left, top, left + width, top);
	contentStream.drawLine(left + width, top, left + width, top + height);
	contentStream.drawLine(left + width, top + height, left, top + height);
	contentStream.drawLine(left, top + height, left, top);
    }
}