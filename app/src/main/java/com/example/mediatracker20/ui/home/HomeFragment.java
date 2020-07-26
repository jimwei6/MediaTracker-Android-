package com.example.mediatracker20.ui.home;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.selection.OnDragInitiatedListener;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediatracker20.R;
import com.example.mediatracker20.listselectors.ActionModeController;
import com.example.mediatracker20.listselectors.KeyProvider;
import com.example.mediatracker20.listselectors.ListItemLookup;
import com.example.mediatracker20.adapters.MediaListAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import model.Organize.Sorter;
import model.exceptions.EmptyStringException;
import model.exceptions.KeyAlreadyExistsException;
import model.jsonreaders.ReadUserItem;
import model.model.ListManager;
import model.model.MediaList;
import model.persistence.Reader;
import model.persistence.Writer;

//handles the main menu -> where all lists are displayed and interacted
public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    private View rootView;

    private List<MediaList> allLists;
    private ListManager listManager;
    private MediaListAdapter mediaListAdapter;
    private RecyclerView recyclerView;
    private Spinner spinner;

    private Dialog newListDialog;

    private ActionMode actionMode;
    private Menu menu;

    private FloatingActionButton fab;

    private  SearchView searchView;

    //Handle view creation and ui actions
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        listManager = ListManager.getInstance();
        try {
            processListData(Reader.readListFile(getContext().getFilesDir().getPath() + "/listFile.txt"), listManager);
        } catch (KeyAlreadyExistsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        newListDialog = new Dialog(getContext());
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        searchView = rootView.findViewById(R.id.item_search);
        searchView.setQueryHint("Search");
        searchView.onActionViewExpanded();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mediaListAdapter.filter(newText);
                return false;
            }
        });
        initializeSpinner();
        initializeRecyclerView();
        initializeSelectionTracker();
        initializeFab();
        return rootView;
    }

    private static void processListData(ArrayList<MediaList> listRead, ListManager listColl) throws KeyAlreadyExistsException {
        if (listRead != null) {
            for (MediaList list: listRead) {
                listColl.addNewList(list);
            }
        } else {
            System.out.println("There are no lists");
        }
    }

    //initialize list of cardViews to show all lists
    private void initializeRecyclerView() {
        recyclerView = rootView.findViewById(R.id.card_display);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        allLists = listManager.allActiveLists().stream().collect(Collectors.toList());
        mediaListAdapter = new MediaListAdapter(allLists);
        recyclerView.setAdapter(mediaListAdapter);
    }

    //initialize spinner for different sorting methods
    private void initializeSpinner() {
        spinner = rootView.findViewById(R.id.sort_choice);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.list_spinner_array, R.layout.spinner_item_text);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortList(allLists);
                searchView.clearFocus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Sorter.listSortByName(allLists);
            }
        });
    }

    //initialize a selection tracker to allow multiple item selections
    private void initializeSelectionTracker() {
        SelectionTracker selectionTracker = new SelectionTracker.Builder<>(
                "my-selection-id",
                recyclerView,
                new KeyProvider(1, allLists),
                new ListItemLookup(recyclerView),
                StorageStrategy.createLongStorage()
        )

                .withOnDragInitiatedListener(new OnDragInitiatedListener() {
                    @Override
                    public boolean onDragInitiated(@NonNull MotionEvent e) {
                        Log.d("drag", "onDragInitiated");
                        return true;
                    }
                }).build();
        mediaListAdapter.setSelectionTracker(selectionTracker);
        selectionTracker.addObserver(new SelectionTracker.SelectionObserver() {
            @Override
            public void onSelectionChanged() {
                super.onSelectionChanged();
                if (selectionTracker.hasSelection() && actionMode == null) {
                actionMode = getActivity().startActionMode(new ActionModeController(getActivity(),
                            selectionTracker, allLists, mediaListAdapter));
                    Integer size = selectionTracker.getSelection().size();
                    actionMode.setTitle(size.toString());
                    fab.setClickable(false);
                    fab.hide();
                    searchView.clearFocus();
                    searchView.setInputType(InputType.TYPE_NULL);
                } else if (!selectionTracker.hasSelection() && actionMode != null) {
                    actionMode.finish();
                    actionMode = null;
                    fab.setClickable(true);
                    fab.show();
                    searchView.setInputType(InputType.TYPE_CLASS_TEXT);
                } else if (selectionTracker.hasSelection()){
                    Integer size = selectionTracker.getSelection().size();
                    actionMode.setTitle(size.toString());
                }
            }
        });

    }

    //initialize fab to for new item creation.
    private void initializeFab() {
        fab = rootView.findViewById(R.id.newListButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchView.clearFocus();
                newListDialog.setContentView(R.layout.new_list_popup);
                newListDialog.show();
                Button confirm = newListDialog.findViewById(R.id.confirmButton);
                TextView errorText = newListDialog.findViewById(R.id.errorText);
                errorText.setVisibility(View.GONE);
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        searchView.setQuery("", false);
                        EditText name = newListDialog.findViewById(R.id.newListName);
                        try {
                            listManager.addNewList(new MediaList(name.getText().toString()));
                            allLists.add(new MediaList(name.getText().toString()));
                            newListDialog.dismiss();
                            mediaListAdapter.syncLists();
                            sortList(allLists);
                        } catch (KeyAlreadyExistsException e) {
                            errorText.setVisibility(View.VISIBLE);
                            errorText.setText(getString(R.string.list_already_exist));
                        } catch (EmptyStringException e) {
                            errorText.setVisibility(View.VISIBLE);
                            errorText.setText(getString(R.string.list_empty_name));
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        this.menu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                try {
                    File listFile = new File(getContext().getFilesDir(), "listFile.txt");
                    File tagFile = new File(getContext().getFilesDir(), "tagFile.txt");
                    File itemFile = new File(getContext().getFilesDir(), "itemFile.txt");
                    Writer writer = new Writer(listFile, tagFile, itemFile);
                    writer.write(ListManager.getInstance());
                    writer.close();
                } catch (IOException | JSONException e) {
                    Log.d("fail save", "save failed");
                    e.printStackTrace();
                };

        }
        return super.onOptionsItemSelected(item);
    }

    //sort list given based on the current spinner value
    private void sortList(List<MediaList> allLists) {
        switch (spinner.getSelectedItem().toString()) {
            case "Recent":
                Sorter.listSortByLatestAccess(allLists);
                break;
            case "Date Created":
                Sorter.listSortByDateCreated(allLists);
                break;
            case "Most Used":
                Sorter.listSortByFrequency(allLists);
                break;
            default:
                Sorter.listSortByName(allLists);
                break;
        }
        mediaListAdapter.notifyDataSetChanged();
    }


}