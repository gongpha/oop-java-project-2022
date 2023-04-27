package oop.project.grouppe.y2022;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;

public interface DungeonGenerator {
	public long getSeed();
	public void startGenerate();
	public TiledMap getMap();
	
	public Vector2[] getPaperSpawns();
	public Vector2[] getPowerSpawns();
	
	public float getSpawnPointX();
	public float getSpawnPointY();
	public float getEnemySpawnPointX();
	public float getEnemySpawnPointY();
	public byte[][] getColTiles2DArray();
}
