package caldwell.ben.bites;

import java.util.ArrayList;

import caldwell.ben.bites.RecipeBook.Ingredients;
import caldwell.ben.bites.RecipeBook.Recipes;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class IngredientList extends ListActivity {
	
	private static final String TAG = "IngredientList";
	
	// Menu item ids
	public static final int MENU_ITEM_EDIT = Menu.FIRST;
	public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;
    public static final int MENU_ITEM_INSERT = Menu.FIRST + 2;
    public static final int MENU_ITEM_CHECK = Menu.FIRST + 3;
    public static final int MENU_ITEM_SEND = Menu.FIRST + 4;
    public static final int MENU_ITEM_SHOP_LIST = Menu.FIRST + 5;
	
	/**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION = new String[] {
            Ingredients._ID, // 0
            Ingredients.RECIPE, // 1
            Ingredients.TEXT, // 2
            Ingredients.STATUS, // 3
    };
    
    /**
     * Column indexes
     */
    private static final int COLUMN_INDEX_ID = 0;
//    private static final int COLUMN_INDEX_RECIPE = 1;
    private static final int COLUMN_INDEX_INGREDIENT = 2;
    
    /**
     * Case selections for the type of dialog box displayed
     */
    private static final int DIALOG_EDIT = 1;
    private static final int DIALOG_DELETE = 2;
    private static final int DIALOG_INSERT = 3;
       
    private Uri mUri;
    
  //Use private members for dialog textview to prevent weird persistence problem
	private EditText mDialogEdit;
	private View mDialogView;
	private TextView mDialogText;
	private TextView mHeader;

	private Cursor mCursor;
	private long mLastRecipe;

	/**
	 * IngredientAdapter is a custom cursor adapter for ingredient lists.
	 * 
	 * @author Ben
	 *
	 */
	private class IngredientAdapter extends SimpleCursorAdapter implements Filterable
    {
    	private ContentResolver mContent;   
    	
		public IngredientAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
			mContent = context.getContentResolver();
			
		}

		@Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if (getFilterQueryProvider() != null) {
                return getFilterQueryProvider().runQuery(constraint);
            }

            StringBuilder buffer = null;
            String[] args = null;
            if (constraint != null) {
                buffer = new StringBuilder();
                buffer.append("UPPER(");
                buffer.append(Recipes.TITLE);
                buffer.append(") GLOB ?");
                args = new String[] { constraint.toString().toUpperCase() + "*" };
            }

            return mContent.query(Ingredients.CONTENT_URI, PROJECTION,
                    buffer == null ? null : buffer.toString(), args,
                    null);
        }

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView item = (TextView)view.findViewById(R.id.ingredienttext);
			CheckBox cb = (CheckBox)view.findViewById(R.id.ingredientcheck);
			item.setText(cursor.getString(cursor.getColumnIndex(Ingredients.TEXT)));
			switch(cursor.getInt(cursor.getColumnIndex(Ingredients.STATUS))){
			case Ingredients.STATUS_CHECKED:
				cb.setChecked(true);
				break;
			case Ingredients.STATUS_UNCHECKED:
				cb.setChecked(false);
				break;
			}
		}
		
		
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
			
		Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Ingredients.CONTENT_URI);
        }
			
		setContentView(R.layout.ingredients);
		mHeader = (TextView)findViewById(R.id.ingredientheader);
		getListView().setOnCreateContextMenuListener(this);		
		mLastRecipe = 0;
	}
		
	@Override
	protected void onResume() {
		super.onResume();
		/**Refresh the cursor using the selected recipe whenever the activity is resumed.
		 * A new recipe can only be selected from the recipelist activity and 
		 * this activity has to be resumed to display again so this should work fine. 
		 */
		mCursor = managedQuery(Ingredients.CONTENT_URI, PROJECTION,
				Ingredients.RECIPE + "=" + Bites.mRecipeId, 
				null, Ingredients.DEFAULT_SORT_ORDER);
		
		//If this is a different recipe to last time set all ingredients to unchecked
		if (mLastRecipe != Bites.mRecipeId) {
			ContentValues values = new ContentValues();
			values.put(Ingredients.STATUS, Ingredients.STATUS_UNCHECKED);
			getContentResolver().update(getIntent().getData(), 
										values, 
										Ingredients.RECIPE + "=" + Bites.mRecipeId, 
										null);
			//Remember this recipe id for checking for a new recipe next time
			mLastRecipe = Bites.mRecipeId;
			mCursor.requery();
		}

		// Used to map notes entries from the database to views
		IngredientAdapter adapter = new IngredientAdapter(this, R.layout.ingredientlist_item, mCursor,
		new String[] { Ingredients.TEXT, Ingredients.STATUS}, 
						new int[] { R.id.ingredienttext, R.id.ingredientcheck});
		setListAdapter(adapter);
			
		//Set the header text to the current recipe name
		mHeader.setText(Bites.mRecipeName);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }
		Cursor cursor = (Cursor)getListAdapter().getItem(info.position);
		if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }
        // Setup the menu header
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_INGREDIENT));
        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_EDIT, 0, R.string.edit_ingredient);
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.delete_ingredient);
	}	

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }
        
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return false;
        }
        
        mUri = ContentUris.withAppendedId(getIntent().getData(), cursor.getLong(COLUMN_INDEX_ID));

        switch (item.getItemId()) {
	        case MENU_ITEM_EDIT: {
                // Edit the ingredient that the context menu is for
	        	showDialog(DIALOG_EDIT);
				mDialogEdit.setText(cursor.getString(COLUMN_INDEX_INGREDIENT));
                return true;	        	
	        }    
	        case MENU_ITEM_DELETE: {
                // Delete the note that the context menu is for
	        	showDialog(DIALOG_DELETE);
				mDialogText.setText(cursor.getString(COLUMN_INDEX_INGREDIENT));
                return true;
            }
        }
        return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		// Insert a new recipe into the list
        menu.add(0, MENU_ITEM_INSERT, 1, "insert")
                .setShortcut('3', 'a')
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, MENU_ITEM_EDIT, 2, "edit")
        .setIcon(android.R.drawable.ic_menu_edit);
        
     // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        //Intent intent = new Intent(null, getIntent().getData());
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType("vnd.android.cursor.dir/vnd.caldwell.ben.trolly");
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.putStringArrayListExtra("items", getListExtra());
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, IngredientList.class), null, intent, 0, null);
        menu.add(0, MENU_ITEM_SEND, 3, "send")
        .setIcon(android.R.drawable.ic_menu_send);
        menu.add(0, MENU_ITEM_DELETE, 5, "delete")
        .setIcon(android.R.drawable.ic_menu_delete);
        
        return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType("vnd.android.cursor.dir/vnd.caldwell.ben.trolly");
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.putStringArrayListExtra("items", getListExtra());
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 4,
                new ComponentName(this, IngredientList.class), null, intent, 0, null);
		return true;
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		//Shopping list item is not in this switch as it is an intent menu item and does not need to be handled here
		switch (item.getItemId()) {
        case MENU_ITEM_INSERT:
            // Insert a new item
        	showDialog(DIALOG_INSERT);
        	mDialogEdit.setText("");
        	break;
	    case MENU_ITEM_EDIT:
	        // Edit an existing item
			showDialog(DIALOG_EDIT);
			mDialogEdit.setText(mCursor.getString(COLUMN_INDEX_INGREDIENT));
			break;
	    case MENU_ITEM_DELETE:
	        // Edit an existing item
			showDialog(DIALOG_DELETE);
			mDialogText.setText(mCursor.getString(COLUMN_INDEX_INGREDIENT));
			break;
	    case MENU_ITEM_SEND:
	    	/* 
        	 * Create an intent to send a text message (sms). 
        	 * The body of the message is all the ticked/unticked ingredients.
        	 */
    		Intent sendIntent = new Intent(Intent.ACTION_VIEW);
    		sendIntent.putExtra("sms_body", createShoppingList());
        	sendIntent.setType("vnd.android-dir/mms-sms");
        	startActivity(sendIntent);
	    	break;
	    }
        return super.onOptionsItemSelected(item);
    }
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		mUri = ContentUris.withAppendedId(getIntent().getData(), id);
		Cursor c = getContentResolver().query(mUri,PROJECTION, 
												null, 
												null, 
												Ingredients.DEFAULT_SORT_ORDER);
		ContentValues values = new ContentValues();
		c.moveToFirst();
		if (!c.isBeforeFirst()) {
			switch (c.getInt(c.getColumnIndex(Ingredients.STATUS))) {
			case Ingredients.STATUS_CHECKED:
				values.put(Ingredients.STATUS, Ingredients.STATUS_UNCHECKED);
				getContentResolver().update(mUri, values, null, null);
				break;
			case Ingredients.STATUS_UNCHECKED:
				values.put(Ingredients.STATUS, Ingredients.STATUS_CHECKED);
				getContentResolver().update(mUri, values, null, null);
				break;
			}
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		LayoutInflater factory = LayoutInflater.from(this);
		switch (id) {
		case DIALOG_EDIT:
            mDialogView = factory.inflate(R.layout.dialog_ingredient, null);
            mDialogEdit = (EditText)mDialogView.findViewById(R.id.ingredient_edit);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.edit_ingredient)
                .setView(mDialogView)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int whichButton) {
                    	/* User clicked OK so do some stuff */
                    	ContentValues values = new ContentValues();
                        values.put(Ingredients.TEXT, mDialogEdit.getText().toString());
                        values.put(Ingredients.RECIPE, Bites.mRecipeId);
                        getContentResolver().update(mUri, values, null, null);
                	}
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        /* User clicked cancel so do some stuff */
                    }
                })
                .create();
		case DIALOG_DELETE:
			mDialogView = factory.inflate(R.layout.dialog_confirm, null);
			mDialogText = (TextView)mDialogView.findViewById(R.id.dialog_confirm_prompt);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.delete_ingredient)
                .setView(mDialogView)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int whichButton) {
                    	/* User clicked OK so do some stuff */
                		getContentResolver().delete(mUri, null, null);
                	}
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	
                    }
                })
                .create();
		case DIALOG_INSERT:
            mDialogView = factory.inflate(R.layout.dialog_ingredient, null);
            mDialogEdit = (EditText)mDialogView.findViewById(R.id.ingredient_edit);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.insert_ingredient)
                .setView(mDialogView)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                	public void onClick(DialogInterface dialog, int whichButton) {
                    	/* User clicked OK so do some stuff */
                		ContentValues values = new ContentValues();
                		values.put(Ingredients.RECIPE, Bites.mRecipeId);
                		mUri = getContentResolver().insert(Ingredients.CONTENT_URI,values);
                        values.put(Ingredients.TEXT, mDialogEdit.getText().toString());
                        values.put(Ingredients.RECIPE, Bites.mRecipeId);
                        getContentResolver().update(mUri, values, null, null);
                	}
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        /* User clicked cancel so do some stuff */
                    }
                })
                .create();
		}
		return null;
	}
	
	/**
	 * Send Shopping List via sms.
	 * 
	 * <P>Create a shopping list text string for sending via sms, 
	 * and load an intent with data for a shopping list activity. </P>
	 */
	private String createShoppingList()
	{
		String msg = "***Shopping List***\n";
		ListView lv = getListView();
		int lvCount = lv.getChildCount();
		for (int i=0; i<lvCount; i++)
		{
			CheckBox cb = (CheckBox)lv.getChildAt(i).findViewById(R.id.ingredientcheck);
			//Send the checked items (ones we don't have) via sms
			if (cb.isChecked())
			{
				msg = msg + ((TextView)lv.getChildAt(i).findViewById(R.id.ingredienttext)).getText() + "\n";
			}
		}
		msg = msg + "***";
		return msg;
	}
	
	/**
	 * Get a string list of ingredients to use for an intent to add items to a shopping list activity.
	 * @return
	 */
	private ArrayList<String> getListExtra() {
		ArrayList<String> list = new ArrayList<String>();
		ListView lv = getListView();
		int lvCount = lv.getChildCount();
		for (int i=0; i<lvCount; i++)
		{
			CheckBox cb = (CheckBox)lv.getChildAt(i).findViewById(R.id.ingredientcheck);
			//Send checked ingredients (the ones we don't have)
			if (cb.isChecked())
			{
				 list.add((String) ((TextView)lv.getChildAt(i).findViewById(R.id.ingredienttext)).getText());
			}
		}
		return list;
	}
	
}

	
