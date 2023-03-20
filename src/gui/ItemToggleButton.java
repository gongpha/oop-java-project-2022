public class ItemToggleButton extends Sprite {
	private Game game;
	public ItemToggleButton(Game game) {
		this.game = game;
	}
	
	public void onPressed() {
		game.toggleItems();
	}
}
