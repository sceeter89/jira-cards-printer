package eu.ganymede.jira.cardsPrinter;

public class CardInformation {
    private final String key;
    private final String summary;
    private final int storyPoints;
    private final int subtasks;
    
    public CardInformation(String key,
	String summary,
	int storyPoints,
	int subtasks) {
	
	this.key = key;
	this.summary = summary;
	this.storyPoints = storyPoints;
	this.subtasks = subtasks;
    }
    
    public String getKey() { return this.key; }
    public String getSummary() { return this.summary; }
    public int getStoryPoints() { return this.storyPoints; }
    public int getSubtasks() { return this.subtasks; }
}