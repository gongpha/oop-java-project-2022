package oop.project.grouppe.y2022;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

// demo file writer

public class DemoWriter implements DemoHandler {
	private File demoFile;
	private DataOutputStream demoFileWriter = null;
	
	private World world;
	
	private boolean dead; // like the thread state, IT ISN'T WORKING ANYMORE
	
	public DemoWriter(World world) {
		this.world = world;
		
		// init writer
		try {
			demoFile = new File("demo" + world.getGameSeed() + ".oopdemo");
			demoFile.createNewFile();
			demoFileWriter = new DataOutputStream(new FileOutputStream(demoFile));

			// magic numbers "OOPDEMO1"
			demoFileWriter.writeInt(DEMOMAGIC1);
			demoFileWriter.writeInt(DEMOMAGIC2);

			// seed
			demoFileWriter.writeLong(world.getGameSeed());

			demoWriteAllEntityInfos();
			demoWritePlayerInfos();

		} catch (IOException e) {
			CoreGame.instance().getConsole().printerr("Cannot recording : " + e.getMessage());
		}
	}
	
	public boolean isDead() {
		return dead;
	}
	
	private void demoWritingFailure(IOException e) {
		CoreGame.instance().getConsole().printerr("Demo writing failure : " + e.getMessage());
		demoFile.delete();
		demoFileWriter = null;
		demoFile = null;
		dead = true;
	}
	
	private void demoWriteAllEntityInfos() {
		try {
			Entity[] entities = world.dumpAllEntities();
			demoFileWriter.writeInt(entities.length);
			for (Entity ent : entities) {
				demoFileWriter.writeInt(ent.getID());
				demoFileWriter.writeUTF(ent.getClass().getName());
				ent.serializeConstructor(demoFileWriter);
			}
		} catch (IOException e) {
			demoWritingFailure(e);
		}
		
	}
	
	private void demoWritePlayerInfos() {
		// write all player infos (name, customization)
		Character c = world.getSpectatingCharacter();
		try {
			demoFileWriter.writeInt(world.getCharacterCount());
			for (int i = 0; i < world.getCharacterCount(); i++) {
				Character ch = world.getCharacterByIndex(i);
				Player player = ch.getPlayer();
				demoFileWriter.writeInt(player.getNetID());
				demoFileWriter.writeInt(ch.getID());
				player.writeStream(demoFileWriter);
			}
			demoFileWriter.writeInt(c.getPlayer().getNetID());
		} catch (IOException e) {
			demoWritingFailure(e);
		}
	}
	
	public void processRecordEntityProcess(int frameProcessed, Entity ent) {
		try {
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(ba);
			
			if (ent.serializeRecord(dos)) {
				demoFileWriter.writeInt(frameProcessed);
				demoFileWriter.writeByte(0x01);

				demoFileWriter.writeInt(ent.getID());
				dos.flush();
				demoFileWriter.write(ba.toByteArray());
			}
		} catch (IOException e) {
			demoWritingFailure(e);
		}
		
	}
	
	public void processRecordChat(int frameProcessed, ChatText t, boolean flash) {
		try {
			demoFileWriter.writeInt(frameProcessed);
			demoFileWriter.writeByte(0x02);
			
			demoFileWriter.writeBoolean(flash);
			demoFileWriter.writeUTF(t.getString());
			demoFileWriter.writeUTF(t.getAuthor());
			demoFileWriter.writeUTF(t.getAuthorColor());
		} catch (IOException e) {
			demoWritingFailure(e);
		}
	}
	
	public void processRecordSoundEmit(int frameProcessed, String sndName) {
		try {
			demoFileWriter.writeInt(frameProcessed);
			demoFileWriter.writeByte(0x03);
			
			demoFileWriter.writeUTF(sndName);
		} catch (IOException e) {
			demoWritingFailure(e);
		}
	}
	
	public void processRecordEntityDelete(int frameProcessed, int entID) {
		try {
			demoFileWriter.writeInt(frameProcessed);
			demoFileWriter.writeByte(0x04);
			
			demoFileWriter.writeInt(entID);
		} catch (IOException e) {
			demoWritingFailure(e);
		}
	}
	
	public void processRecordCharacterDied(int frameProcessed, int entID) {
		try {
			demoFileWriter.writeInt(frameProcessed);
			demoFileWriter.writeByte(0x05);
			
			demoFileWriter.writeInt(entID);
		} catch (IOException e) {
			demoWritingFailure(e);
		}
	}
	
	public void processRecordCharacterRevived(int frameProcessed, int entID) {
		try {
			demoFileWriter.writeInt(frameProcessed);
			demoFileWriter.writeByte(0x06);
			
			demoFileWriter.writeInt(entID);
		} catch (IOException e) {
			demoWritingFailure(e);
		}
	}

	public void finishWriting() {
		try {
			demoFileWriter.flush();
			demoFileWriter.close();
			CoreGame.instance().getConsole().print("Finished recording. (" + demoFile.getPath() + ")");
		} catch (IOException e) {
			CoreGame.instance().getConsole().printerr("Cannot finish writing the demo file.");
		} finally {
			demoFileWriter = null;
			demoFile = null;
			dead = true;
		}
	}
}
