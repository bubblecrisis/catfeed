package utils;

import android.view.View;
import android.widget.ListView;

public class ListPosition {
	public int offset = 0;
	public int position = 0;
	
	public void scroll(ListView view) {
		view.setSelectionFromTop(position, offset);
	}
	
	public static ListPosition getScrollPosition(ListView view) {
		ListPosition position = new ListPosition();
		position.position = view.getFirstVisiblePosition();
		View firstItem = view.getChildAt(0);
		position.offset = (firstItem == null) ? 0 : firstItem.getTop();
		return position;
	}
}