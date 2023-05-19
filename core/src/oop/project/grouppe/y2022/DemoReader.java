package oop.project.grouppe.y2022;

// demo reader for playback

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class DemoReader implements DemoHandler {
	private final World world;
	private final String demoFileName;
	
	private File demoFile;
	private DataInputStream demoFileReader = null;
	private int waitingFrame = -1;
	
	private boolean dead; // like the thread state, IT ISN'T WORKING ANYMORE
	
	public DemoReader(String demoFileName, World world) {
		this.world = world;
		this.demoFileName = demoFileName;
	}
	
	public final void initializeDemo() {
		if (demoFileName == null) {
			CoreGame.instance().getConsole().printerr("Not initialized yet.");
			dead = true;
			return;
		}
		demoFile = new File(demoFileName);
		if (!demoFile.exists()) {
			CoreGame.instance().getConsole().printerr("File " + demoFileName + " doesn't exist.");
			dead = true;
			return;
		}

		try {
			demoFileReader = new DataInputStream(new FileInputStream(demoFile));
		} catch (FileNotFoundException e) {
			dead = true;
			return;
		}
		
		try {
			int magic = demoFileReader.readInt();
			if (magic != DEMOMAGIC1) {
				CoreGame.instance().getConsole().printerr("DEMO BAD MAGIC 1");
				return;
			}
			magic = demoFileReader.readInt();
			if (magic != DEMOMAGIC2) {
				CoreGame.instance().getConsole().printerr("DEMO BAD MAGIC 2");
				return;
			}
			
			// init the world
			world.initWorldFromDemoFile(demoFileReader);
			
		} catch (IOException e) {
			CoreGame.instance().getConsole().printerr("Cannot read " + demoFileName + " : " + e.getMessage());
			dead = true;
		}
	}

	public boolean isDead() {
		return dead;
	}
	
	private void readProcessRecordEntityProcess() throws IOException {
		int entID = demoFileReader.readInt();
		Entity ent = world.getEntity(entID);
		if (ent == null) {
			demoPlaybackFailure("Entity " + entID + " is not found");
			return;
		}
		ent.deserializeRecord(demoFileReader);
	}
	
	private void readProcessRecordChat() throws IOException {
		boolean flash = demoFileReader.readBoolean();
		String msg = demoFileReader.readUTF();
		String author = demoFileReader.readUTF();
		String authorColor = demoFileReader.readUTF();
		
		ChatText i = new ChatText(msg);
		i.setAuthor(author);
		i.setAuthorColor(authorColor);
		
		world.getHUD().feedTextItem(i);
		
		if (flash)
			CoreGame.instance().flashScreen();
	}
	
	private void readProcessRecordSoundEmit() throws IOException {
		String sndName = demoFileReader.readUTF();
		ResourceManager.instance().playSound(sndName);
	}
	
	private void readProcessRecordEntityDelete() throws IOException {
		int entID = demoFileReader.readInt();
		world.deleteEntity(entID);
	}
	
	private void readProcessRecordCharacterDied() throws IOException {
		int entID = demoFileReader.readInt();
		Entity ent = world.getEntity(entID);
		if (ent instanceof Character) {
			ent.die();
		}
	}
	
	private void readProcessRecordCharacterRevived() throws IOException {
		int entID = demoFileReader.readInt();
		Entity ent = world.getEntity(entID);
		if (ent instanceof Character) {
			ent.revive();
		}
	}
	
	public boolean initializedForDemoPlaying() {
		return demoFileName != null;
	}
	
	public boolean readFrame(int frameProcessed) {
		// PLAYBACK !!!
		while (true) {
			try {
				if (waitingFrame != -1) {
					if (frameProcessed < waitingFrame) {
						// stay waiting
						return false;
					}
					// okay synchronized :>
					waitingFrame = -1;
				} else {
					int keyframeFrame = demoFileReader.readInt();
					if (frameProcessed < keyframeFrame) {
						// ooh, wait for syncing the frame
						waitingFrame = keyframeFrame;
						return false;
					}
				}

				byte frameType = demoFileReader.readByte();
				switch (frameType) {
				case 1:
					readProcessRecordEntityProcess();
					break;
				case 2:
					readProcessRecordChat();
					break;
				case 3:
					readProcessRecordSoundEmit();
					break;
				case 4:
					readProcessRecordEntityDelete();
					break;
				case 5:
					readProcessRecordCharacterDied();
					break;
				case 6:
					readProcessRecordCharacterRevived();
					break;
				default:
					// what ?
					demoPlaybackFailure("Invalid frame type : " + (int)frameType);
				}

			} catch (EOFException e) {
				demoEndPlayback();
				return true; // finished
			} catch (IOException e) {
				demoPlaybackFailure("Cannot read the frame : " + e.getMessage());
				return true;
			}
		}
	}
	
	private void demoPlaybackFailure(String reason) {
		CoreGame.instance().getConsole().print(reason);
		demoEndPlayback();
	}
	
	private void demoEndPlayback() {
		dead = true;
		CoreGame.instance().tellFinishedPlayingDemo();
	}
	
	public void finish() {}
}
