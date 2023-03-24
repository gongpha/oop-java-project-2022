package oop.project.grouppe.y2022;

public abstract class ConsoleCommand {
	private String cmd;
	private Console console;
	public ConsoleCommand(String cmd) {
		this.cmd = cmd;
	}
	
	public String getCmd() {
		return cmd;
	}
	
	public void setConsole(Console c) {
		console = c;
	}
	
	public Console getConsole() {
		return console;
	}
	
	///////////////////////////////////
	// implement your command here
	public abstract void exec(String[] args);
}
