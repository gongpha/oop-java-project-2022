package oop.project.grouppe.y2022;

public class ChatText {
	private String s;
	public void setString(String string) { s = string; }
	public String getString() { return s; }
	
	private String author = "";
	public void setAuthor(String author) { this.author = author; }
	public String getAuthor() { return author; }
	
	private String authorColor = "GREEN";
	public void setAuthorColor(String authorColor) { this.authorColor = authorColor; }
	public String getAuthorColor() { return authorColor; }
	
	private float time;
	public void setTime(float time) { this.time = time; }
	public float getTime() { return time; }
	
	public ChatText(String text) {
		s = text.replace("[", "[[");
	}
}
