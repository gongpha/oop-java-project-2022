public class RoomNavigateButton extends Sprite {
	private Game game;
	private Game.NavigateDirection direction;
	public RoomNavigateButton(Game game, Game.NavigateDirection direction) {
		this.game = game;
		this.direction = direction;
		//setVisible(false);
	}
	
	public void onPressed() {
		game.navigate(direction);
	}
}
