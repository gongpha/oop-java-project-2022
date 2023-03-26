package oop.project.grouppe.y2022;

// an object that does like a character 

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;


public class Character extends Entity {
	private CoreGame game;
	private World world;
	private final TextureRegion region;
	private Vector2 velocity;
	private int input = 0;
	
	public Character() {
		game = CoreGame.instance();
		world = game.getWorld();
		
		region = new TextureRegion();
		region.setTexture((Texture) ResourceManager.instance().get("defcharacter"));
		region.setRegionX(0);
		region.setRegionY(32);
		region.setRegionWidth(32);
		region.setRegionHeight(32);
		
		velocity = new Vector2();
	}
	
	public void setInput(int input) {
		this.input = input;
	}
	
	public void process(float delta) {
		World world = getWorld();
		
		velocity.x = world.isPressedInt(World.InputMap.RIGHT) - world.isPressedInt(World.InputMap.LEFT);
		velocity.y = world.isPressedInt(World.InputMap.UP) - world.isPressedInt(World.InputMap.DOWN);
		
		
		move(velocity);
	}
	
	@Override
	public void draw(Batch batch, float alpha){
		batch.draw(region, getX(), getY());
	}
	
	public void setCharacterSheet(Texture texture) {
		region.setTexture(texture);
	}
}
