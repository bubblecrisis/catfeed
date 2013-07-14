package utils;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;

public class FragmentUtils {

	
	/**
	 * Adds a new fragment. 
	 * @param context
	 * @param manager
	 * @param containderId
	 * @param newFragmentClass
	 */
	public static Fragment add(Context context, FragmentManager manager, int containderId, Class<? extends Fragment> newFragmentClass) {
		Fragment newFragment = manager.findFragmentByTag(newFragmentClass.getName());
		if (newFragment == null) newFragment = Fragment.instantiate(context, newFragmentClass.getName());
		add(manager, containderId, newFragment); 
		return newFragment;
	}	
	
	/**
	 * Adds a new fragment. 
	 * @param manager
	 * @param containderId
	 * @param newFragment
	 */
	public static void add(FragmentManager manager, int containderId, Fragment newFragment) {
		FragmentTransaction transaction = manager.beginTransaction();
		String tag = newFragment.getTag();
		if (tag == null) tag = newFragment.getClass().getName();
		transaction.add(containderId, newFragment, tag);
		transaction.commit(); 
	}

	/**
	 * Replace a fragment. 
	 * @param context
	 * @param manager
	 * @param containderId
	 * @param newFragmentClass
	 * @param oldFragment
	 */
	public static void replace(Context context, FragmentManager manager, int containderId, Class<? extends Fragment> newFragmentClass, Fragment oldFragment) {
		Fragment newFragment = manager.findFragmentByTag(newFragmentClass.getName());
		if (newFragment == null) newFragment = Fragment.instantiate(context, newFragmentClass.getName());
		replace(manager, containderId, newFragment, oldFragment); 
	}	
	
	
	/**
	 * Replaces a fragment
	 * @param manager
	 * @param newFragment
	 * @param oldFragment
	 */
	public static void replace(FragmentManager manager, int containderId, Fragment newFragment, Fragment oldFragment) {
		FragmentTransaction transaction = manager.beginTransaction();

		String tag = newFragment.getTag();
		if (tag == null) tag = newFragment.getClass().getName();
		
		// Replace whatever is in the fragment_container view with this fragment,
		// and add the transaction to the back stack so the user can navigate back
		transaction.addToBackStack(null);
		transaction.hide(oldFragment);
		transaction.replace(containderId, newFragment, tag);

		// Commit the transaction
		transaction.commit();
	}
}
