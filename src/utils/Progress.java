package utils;

public interface Progress {

	public void stop();
	
	public void progress(int progress, int of);
	
	public void setCountDown(int countdown);

	public void countup();	
	
	public void countdown();
}
