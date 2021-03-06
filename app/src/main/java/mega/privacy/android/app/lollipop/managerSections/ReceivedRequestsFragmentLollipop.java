package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaContactRequestLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;

public class ReceivedRequestsFragmentLollipop extends Fragment {

	MegaApiAndroid megaApi;
	
	Context context;
	RecyclerView listView;
	MegaContactRequestLollipopAdapter adapterList;

	ImageView emptyImageView;
	LinearLayout emptyTextView;
	TextView emptyTextViewFirst;
	LinearLayoutManager mLayoutManager;

	private ActionMode actionMode;

	float density;
	DisplayMetrics outMetrics;
	Display display;
	
	boolean isList = true;
	ReceivedRequestsFragmentLollipop receivedRequestsFragment = this;
	
	ArrayList<MegaContactRequest> contacts;

	public void activateActionMode(){
		log("activateActionMode");
		if (!adapterList.isMultipleSelect()){
			adapterList.setMultipleSelect(true);
			actionMode = ((AppCompatActivity)context).startSupportActionMode(new ActionBarCallBack());
		}
	}

	/////Multiselect/////
	private class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

			List<MegaContactRequest> requests = adapterList.getSelectedRequest();

			switch(item.getItemId()){
				case R.id.cab_menu_select_all:{
					selectAll();
					actionMode.invalidate();
					break;
				}
				case R.id.cab_menu_unselect_all:{
					clearSelections();
					hideMultipleSelect();
					actionMode.invalidate();
					break;
				}
				case R.id.cab_menu_accept:{
					ContactController cC = new ContactController(context);
					cC.acceptMultipleReceivedRequest(requests);
					break;
				}
				case R.id.cab_menu_ignore:{
					ContactController cC = new ContactController(context);
					cC.ignoreMultipleReceivedRequest(requests);
					break;
				}
				case R.id.cab_menu_decline:{
					ContactController cC = new ContactController(context);
					cC.declineMultipleReceivedRequest(requests);
					break;
				}
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.received_request_action, menu);
			((ManagerActivityLollipop)context).hideFabButton();
			((ManagerActivityLollipop) context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_ACCENT);
			checkScroll();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
            clearSelections();
			adapterList.setMultipleSelect(false);
			((ManagerActivityLollipop)context).showFabButton();
            ((ManagerActivityLollipop) context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_ZERO_DELAY);
			checkScroll();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			List<MegaContactRequest> selected = adapterList.getSelectedRequest();

			if (selected.size() != 0) {
				menu.findItem(R.id.cab_menu_accept).setVisible(true);
				menu.findItem(R.id.cab_menu_ignore).setVisible(true);
				menu.findItem(R.id.cab_menu_decline).setVisible(true);

				MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
				if(selected.size()==adapterList.getItemCount()){
					menu.findItem(R.id.cab_menu_select_all).setVisible(false);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}
				else{
					menu.findItem(R.id.cab_menu_select_all).setVisible(true);
					unselect.setTitle(getString(R.string.action_unselect_all));
					unselect.setVisible(true);
				}

			}
			else{
				menu.findItem(R.id.cab_menu_select_all).setVisible(true);
				menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
			}

			return false;
		}

	}

	public boolean showSelectMenuItem(){
		if (adapterList != null){
			return adapterList.isMultipleSelect();
		}

		return false;
	}

	/*
	 * Clear all selected items
	 */
	public void clearSelections() {
		if(adapterList!=null){
			if(adapterList.isMultipleSelect()){
				adapterList.clearSelections();
			}
		}
	}

	private void updateActionModeTitle() {
		if (actionMode == null || getActivity() == null) {
			return;
		}
		List<MegaContactRequest> users = adapterList.getSelectedRequest();

		Resources res = getResources();
		String format = "%d %s";

		actionMode.setTitle(String.format(format, users.size(),res.getQuantityString(R.plurals.general_num_request, users.size())));

		try {
			actionMode.invalidate();
		} catch (NullPointerException e) {
			e.printStackTrace();
			log("oninvalidate error");
		}
	}

	/*
	 * Disable selection
	 */
	public void hideMultipleSelect() {
		log("hideMultipleSelect");
		if(adapterList!=null){
			adapterList.setMultipleSelect(false);
		}

		if (actionMode != null) {
			actionMode.finish();
		}
	}

	public void selectAll() {
		if (adapterList != null) {
			if (adapterList.isMultipleSelect()) {
				adapterList.selectAll();
			} else {
				adapterList.setMultipleSelect(true);
				adapterList.selectAll();

				actionMode = ((AppCompatActivity) context).startSupportActionMode(new ActionBarCallBack());
			}

			updateActionModeTitle();
		}
	}

	public void itemClick(int position) {
		log("itemClick");
		if (adapterList.isMultipleSelect()){
			adapterList.toggleSelection(position);
			List<MegaContactRequest> users = adapterList.getSelectedRequest();
			if (users.size() > 0){
				updateActionModeTitle();
			}
		}
		else{
			log("not multiple select - show menu");
			MegaContactRequest c = contacts.get(position);
			((ManagerActivityLollipop) context).showReceivedRequestOptionsPanel(c);
		}
	}

	/////END Multiselect/////

	public static ReceivedRequestsFragmentLollipop newInstance() {
		log("newInstance");
		ReceivedRequestsFragmentLollipop fragment = new ReceivedRequestsFragmentLollipop();
		return fragment;
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate");
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}	
		
    }

	public void checkScroll () {
		if (listView != null) {
			if (listView.canScrollVertically(-1) || (adapterList != null && adapterList.isMultipleSelect())) {
				((ManagerActivityLollipop) context).changeActionBarElevation(true);
			}
			else {
				((ManagerActivityLollipop) context).changeActionBarElevation(false);
			}
		}
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {                
        log("onCreateView");

		display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		density  = getResources().getDisplayMetrics().density;

		contacts = megaApi.getIncomingContactRequests();
		if(contacts!=null) {
			//Order by last interaction
			Collections.sort(contacts, new Comparator<MegaContactRequest>() {

				public int compare(MegaContactRequest c1, MegaContactRequest c2) {
					long timestamp1 = c1.getModificationTime();
					long timestamp2 = c2.getModificationTime();

					long result = timestamp2 - timestamp1;
					return (int) result;
				}
			});
		}

    	if (isList){
     
	        View v = inflater.inflate(R.layout.contacts_received_requests_tab, container, false);			
	        listView = (RecyclerView) v.findViewById(R.id.incoming_contacts_list_view);

			listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
			mLayoutManager = new LinearLayoutManager(context);
			listView.setLayoutManager(mLayoutManager);
			listView.setItemAnimator(new DefaultItemAnimator());
			listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);
					checkScroll();
				}
			});

			emptyImageView = (ImageView) v.findViewById(R.id.empty_image_contacts_requests);
			emptyTextView = (LinearLayout) v.findViewById(R.id.empty_text_contacts_requests);
			emptyTextViewFirst = (TextView) v.findViewById(R.id.empty_text_contacts_requests_first);
			
			if (adapterList == null){
				adapterList = new MegaContactRequestLollipopAdapter(context, this, contacts, listView, Constants.INCOMING_REQUEST_ADAPTER);
			}
			else{
				adapterList.setContacts(contacts);
			}
		
			adapterList.setPositionClicked(-1);
			listView.setAdapter(adapterList);
						
			if (adapterList.getItemCount() == 0){				
				log("adapterList.getCount() == 0");

				listView.setVisibility(View.GONE);
				emptyImageView.setVisibility(View.VISIBLE);
				emptyTextView.setVisibility(View.VISIBLE);

				if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
					emptyImageView.setImageResource(R.drawable.received_empty_landscape);
				}else{
					emptyImageView.setImageResource(R.drawable.received_requests_empty);
				}

				String textToShow = String.format(getString(R.string.received_requests_empty)).toUpperCase();
				try{
					textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
					textToShow = textToShow.replace("[/A]", "</font>");
					textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
					textToShow = textToShow.replace("[/B]", "</font>");
				}
				catch (Exception e){}
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}
				emptyTextViewFirst.setText(result);
			}
			else{
				log("adapterList.getCount() NOT = 0");
				listView.setVisibility(View.VISIBLE);
				emptyImageView.setVisibility(View.GONE);
				emptyTextView.setVisibility(View.GONE);
			}
						
			return v;
    	}
    	else{

    	    View v = inflater.inflate(R.layout.contacts_received_requests_tab, container, false);
    	    return v;
    	}		

    }

	private static void log(String log) {
		Util.log("ReceivedRequestsFragmentLollipop", log);
	}

	public void updateView(){
		contacts = megaApi.getIncomingContactRequests();
		if(contacts!=null){
			//Order by last interaction
			Collections.sort(contacts, new Comparator<MegaContactRequest>(){

				public int compare(MegaContactRequest c1, MegaContactRequest c2) {
					long timestamp1 = c1.getModificationTime();
					long timestamp2 = c2.getModificationTime();

					long result = timestamp2 - timestamp1;
					return (int)result;
				}
			});
		}

		if (adapterList == null){
			adapterList = new MegaContactRequestLollipopAdapter(context, this, contacts, listView, Constants.INCOMING_REQUEST_ADAPTER);
			listView.setAdapter(adapterList);
		}
		else{
			adapterList.setContacts(contacts);
		}
	
		adapterList.setPositionClicked(-1);
		
		if (adapterList.getItemCount() == 0){				
			log("adapterList.getCount() == 0");
			listView.setVisibility(View.GONE);
			emptyImageView.setVisibility(View.VISIBLE);
			emptyTextView.setVisibility(View.VISIBLE);

			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				emptyImageView.setImageResource(R.drawable.received_empty_landscape);
			}else{
				emptyImageView.setImageResource(R.drawable.received_requests_empty);
			}
			String textToShow = String.format(getString(R.string.received_requests_empty).toUpperCase());
			try{
				textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
				textToShow = textToShow.replace("[/A]", "</font>");
				textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
				textToShow = textToShow.replace("[/B]", "</font>");
			}
			catch (Exception e){}
			Spanned result = null;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
			} else {
				result = Html.fromHtml(textToShow);
			}
			emptyTextViewFirst.setText(result);
		}
		else{
			log("adapterList.getCount() NOT = 0");
			listView.setVisibility(View.VISIBLE);
			emptyImageView.setVisibility(View.GONE);
			emptyTextView.setVisibility(View.GONE);
		}		
	}
	
	public void setPositionClicked(int positionClicked){
		if (isList){
			if (adapterList != null){
				adapterList.setPositionClicked(positionClicked);
			}
		}
		else{
//			if (adapterGrid != null){
//				adapterGrid.setPositionClicked(positionClicked);
//			}	
		}		
	}
	
	public void notifyDataSetChanged(){
		if (isList){
			if (adapterList != null){
				adapterList.notifyDataSetChanged();
			}
		}
		else{
//			if (adapterGrid != null){
//				adapterGrid.notifyDataSetChanged();
//			}
		}
	}
	
	public int getItemCount(){
		if(adapterList!=null){
			return adapterList.getItemCount();
		}
		return 0;
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }
}
