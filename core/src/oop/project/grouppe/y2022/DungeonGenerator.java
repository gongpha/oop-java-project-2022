package oop.project.grouppe.y2022;

import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;

public interface DungeonGenerator {
	public long getSeed();
	public void startGenerate();
	public void startGenerateInstant();
	public TiledMap getMap();
	
	public Vector2[] getPaperSpawns();
	public Vector2[] getMedkitSpawns();
	public Vector2[] getPowerSpawns();
	
	public Vector2[] getSpawnPoints();
	public Vector2[] getEnemySpawnPoints();
	public byte[][] getColTiles2DArray();
	public RectangleMapObject getEntranceRect();
}
