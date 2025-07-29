package emm.sys;

import android.database.Cursor;
import android.os.Bundle;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ViewTodayInventoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private DBHelper dbHelper;
    private SearchView searchView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_today_inventory, container, false);

        recyclerView = view.findViewById(R.id.userRecyclerView);
        searchView = view.findViewById(R.id.search);
        dbHelper = new DBHelper(getActivity());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new InventoryAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Load today's inventory
        loadTodayInventory();

        // Setup search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });

        return view;
    }

    private void loadTodayInventory() {
        Cursor cursor = dbHelper.getTodayInventory();
        List<InventoryItem> items = new ArrayList<>();

        if (cursor != null) {
            try {
                int productNameIndex = cursor.getColumnIndexOrThrow("product_name");
                int weightIndex = cursor.getColumnIndexOrThrow("weight");
                int flavourIndex = cursor.getColumnIndexOrThrow("flavour");
                int quantityIndex = cursor.getColumnIndexOrThrow("quantity");
                int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");

                if (cursor.moveToFirst()) {
                    do {
                        InventoryItem item = new InventoryItem(
                                cursor.getString(productNameIndex),
                                cursor.getString(weightIndex),
                                cursor.getString(flavourIndex),
                                cursor.getInt(quantityIndex),
                                cursor.getString(timestampIndex)
                        );
                        items.add(item);
                    } while (cursor.moveToNext());
                }
            } catch (IllegalArgumentException e) {
                Log.e("DB_ERROR", "Missing column in cursor", e);
                Toast.makeText(getActivity(), "Database error occurred", Toast.LENGTH_SHORT).show();
            } finally {
                cursor.close();
            }
        }

        adapter.updateList(items);
    }

    private void filter(String text) {
        List<InventoryItem> filteredList = new ArrayList<>();
        Cursor cursor = dbHelper.getTodayInventory();

        if (cursor != null) {
            try {
                int productNameIndex = cursor.getColumnIndexOrThrow("product_name");
                int weightIndex = cursor.getColumnIndexOrThrow("weight");
                int flavourIndex = cursor.getColumnIndexOrThrow("flavour");
                int quantityIndex = cursor.getColumnIndexOrThrow("quantity");
                int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");

                if (cursor.moveToFirst()) {
                    do {
                        String productName = cursor.getString(productNameIndex);
                        if (productName.toLowerCase().contains(text.toLowerCase())) {
                            filteredList.add(new InventoryItem(
                                    productName,
                                    cursor.getString(weightIndex),
                                    cursor.getString(flavourIndex),
                                    cursor.getInt(quantityIndex),
                                    cursor.getString(timestampIndex)
                            ));
                        }
                    } while (cursor.moveToNext());
                }
            } catch (IllegalArgumentException e) {
                Log.e("DB_ERROR", "Missing column in cursor", e);
            } finally {
                cursor.close();
            }
        }

        adapter.updateList(filteredList);
    }
}