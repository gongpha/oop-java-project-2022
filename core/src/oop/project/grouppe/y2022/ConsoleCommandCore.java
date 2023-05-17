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
					CoreGame.instance().quit();
				}
			},
			new ConsoleCommand("quit") { // ^^^
				public void exec(String[] args) {
					CoreGame.instance().quit();
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
					for (String[] s : Developers.names) {
						console.print("    " + s[0] + "    " + s[1]);
					}
					console.printSep();
				}
			},
			new ConsoleCommand("host") {
				public void exec(String[] args) {
					CoreGame.instance().hostGame();
				}
			},
			new ConsoleCommand("join") {
				public void exec(String[] args) {
					CoreGame g = CoreGame.instance();
					g.joinGame(g.getMenu().getIP());
				}
			},
			new ConsoleCommand("mmnu_customize_host") {
				public void exec(String[] args) {
					CoreGame.instance().getMenu().showCustomize(true);
				}
			},
			new ConsoleCommand("mmnu_customize_join") {
				public void exec(String[] args) {
					CoreGame.instance().getMenu().showCustomize(false);
				}
			},
			new ConsoleCommand("menu_settings") {
				public void exec(String[] args) {
					CoreGame.instance().getMenu().showSettings();
				}
			},
			new ConsoleCommand("mmnu_credits") {
				public void exec(String[] args) {
					CoreGame.instance().getMenu().showCredits();
				}
			},
			new ConsoleCommand("menu_toggle") {
				public void exec(String[] args) {
					CoreGame.instance().getMenu().toggle();
				}
			},
			new ConsoleCommand("flash") {
				public void exec(String[] args) {
					CoreGame.instance().flashScreen();
				}
			},
			new ConsoleCommand("disconnect") {
				public void exec(String[] args) {
					CoreGame.instance().disconnect();
				}
			},
			new ConsoleCommand("showinfo") {
				public void exec(String[] args) {
					CoreGame.instance().toggleShowinfo();
				}
			},
			new ConsoleCommand("zoomout") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.getCamera().zoom = 10.0f;
					}
				}
			},
			// zoom further
			new ConsoleCommand("zoomoutt") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.getCamera().zoom = 30.0f;
					}
				}
			},
			// reset zoom
			new ConsoleCommand("zoomdef") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.getCamera().zoom = 1.0f;
					}
				}
			},
			new ConsoleCommand("showquadtree") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.toggleDrawQuadTree();
					}
				}
			},
			new ConsoleCommand("showpath") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.setDrawPathEnabled(!w.isDrawPathEnabled());
					}
				}
			},
			new ConsoleCommand("d_zoom") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.getCamera().zoom = 10.0f;
						w.setDrawPathEnabled(true);
						w.setDrawQuadTree(true);
					}
					CoreGame.instance().setShowinfo(true);
				}
			},
			new ConsoleCommand("noclip") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.getMyClient().getCharacter().toggleNoclip();
					}
				}
			},
			new ConsoleCommand("god") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.tellToggleCheat(0);
					}
				}
			},
			new ConsoleCommand("buddha") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.tellToggleCheat(1);
					}
				}
			},
			new ConsoleCommand("notarget") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.tellToggleCheat(2);
					}
				}
			},
			new ConsoleCommand("forcewin") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.dForceWin();
					}
				}
			},
			new ConsoleCommand("forcenext") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.newLevel();
					}
				}
			},
			new ConsoleCommand("kill") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.killCharacter(w.getMyClient().getMyPlayer().getNetID());
					}
				}
			},
			new ConsoleCommand("record") {
				public void exec(String[] args) {
					World w = CoreGame.instance().getWorld();
					if (w != null) {
						w.markRecord();
					}
				}
			}
			///////////////////////////////////////
			// ADD YOUR COMMANDS HERE
		};
	}
}
