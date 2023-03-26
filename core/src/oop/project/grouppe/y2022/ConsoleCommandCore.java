package oop.project.grouppe.y2022;

public class ConsoleCommandCore {
	public static ConsoleCommand[] regCommands() {
		return new ConsoleCommand[]{
			///////////////////////////////////////
			new ConsoleCommand("cover") {
				public void exec(String[] args) {
					getConsole().showFull();
				}
			},
			new ConsoleCommand("clear") {
				public void exec(String[] args) {
					getConsole().clear();
				}
			},
			new ConsoleCommand("exit") {
				public void exec(String[] args) {
					System.exit(-1);
				}
			},
			new ConsoleCommand("quit") { // ^^^
				public void exec(String[] args) {
					System.exit(-1);
				}
			},
			new ConsoleCommand("help") {
				public void exec(String[] args) {
					getConsole().print("ASK THE DEVELOPER LMAO. (type \"developers\")");
				}
			},
			new ConsoleCommand("developers") {
				public void exec(String[] args) {
					Console console = getConsole();
					console.printSep();
					console.print("OOP Project Grouppe (OOP Java Project 2022)");
					console.print("--- brought to you by . . . ---");
					console.print("    65070013    Kongfa Waroros");
					console.print("    65070024    Kullapat Kematorn");
					console.print("    65070110    Nakarin Tiprat");
					console.print("    65070231    Soraphon Natnan");
					console.print("    65070245    Harrit Ide");
					console.printSep();
				}
			},
			new ConsoleCommand("host") {
				public void exec(String[] args) {
					CoreGame.instance().hostGame();
				}
			}
			///////////////////////////////////////
			// ADD YOUR COMMANDS HERE
		};
	}
}
