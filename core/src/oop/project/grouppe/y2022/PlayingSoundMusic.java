package oop.project.grouppe.y2022;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

public class PlayingSoundMusic {
	private Disposable sm;
	public Disposable getSM() { return sm; }
	private long i;
	private float selfVolume;
	
	public PlayingSoundMusic(Disposable soundMusic, long index) {
		sm = soundMusic;
		i = index;
		selfVolume = 1.0f;
	}
	
	public void resetGlobalVolume() {
		ResourceManager rm = ResourceManager.instance();
		if (sm instanceof Music) {
			((Music) sm).setVolume(rm.getVolume() * rm.getMusicVolume() * selfVolume);
		}
		if (sm instanceof Sound) {
			((Sound) sm).setVolume(i, rm.getVolume() * selfVolume);
		}
	}
	
	public void setSelfVolume(float vol) {
		selfVolume = vol;
		resetGlobalVolume();
	}
	
	public void stop() {
		if (sm instanceof Music) {
			((Music) sm).stop();
		}
		if (sm instanceof Sound) {
			((Sound) sm).stop();
		}
	}
	
	public boolean isMusic() {
		return sm instanceof Music;
	}
}
