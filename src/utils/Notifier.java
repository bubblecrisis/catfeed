package utils;

import java.util.Observable;

public class Notifier extends Observable {

	@Override
	public void setChanged() {
		super.setChanged();
	}

	@Override
	public void notifyObservers() {
		setChanged();
		super.notifyObservers();
	}

	@Override
	public void notifyObservers(Object data) {
		setChanged();
		super.notifyObservers(data);
	}

	
	
}
