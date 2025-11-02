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

public class ViewIssuedGoodsFragment extends Fragment implements IssuedGoodsAdapter.OnItemActionListener {
    private RecyclerView goodsRecyclerView;
    private IssuedGoodsAdapter adapter;
    private DBHelper dbHelper;
    private SearchView searchView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_issued_goods, container, false);

        // Initialize views
        goodsRecyclerView = view.findViewById(R.id.goodsRecyclerView);
        searchView = view.findViewById(R.id.search);
        dbHelper = new DBHelper(getActivity());

        // Setup RecyclerView
        goodsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new IssuedGoodsAdapter(new ArrayList<>(), this);
        goodsRecyclerView.setAdapter(adapter);

        // Load all issued goods
        loadIssuedGoods();

        // Setup search functionality
        setupSearchView();

        return view;
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterIssuedGoods(newText);
                return true;
            }
        });
    }

    private void loadIssuedGoods() {
        Cursor cursor = dbHelper.getAllIssuedGoods();
        List<IssuedGoodsItem> items = new ArrayList<>();

        if (cursor != null) {
            try {
                int idIndex = cursor.getColumnIndexOrThrow("id");
                int assigneeIndex = cursor.getColumnIndexOrThrow("assignee");
                int productNameIndex = cursor.getColumnIndexOrThrow("product_name");
                int weightIndex = cursor.getColumnIndexOrThrow("weight");
                int flavourIndex = cursor.getColumnIndexOrThrow("flavour");
                int quantityIndex = cursor.getColumnIndexOrThrow("quantity");
                int stationIndex = cursor.getColumnIndexOrThrow("station");
                int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");

                if (cursor.moveToFirst()) {
                    do {
                        IssuedGoodsItem item = new IssuedGoodsItem(
                                cursor.getInt(idIndex),
                                cursor.getString(assigneeIndex),
                                cursor.getString(productNameIndex),
                                cursor.getString(weightIndex),
                                cursor.getString(flavourIndex),
                                cursor.getInt(quantityIndex),
                                cursor.getString(stationIndex),
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

        if (items.isEmpty()) {
            Toast.makeText(getActivity(), "No issued goods found", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterIssuedGoods(String searchText) {
        List<IssuedGoodsItem> filteredList = new ArrayList<>();
        Cursor cursor = dbHelper.getAllIssuedGoods();

        if (cursor != null) {
            try {
                int idIndex = cursor.getColumnIndexOrThrow("id");
                int assigneeIndex = cursor.getColumnIndexOrThrow("assignee");
                int productNameIndex = cursor.getColumnIndexOrThrow("product_name");
                int weightIndex = cursor.getColumnIndexOrThrow("weight");
                int flavourIndex = cursor.getColumnIndexOrThrow("flavour");
                int quantityIndex = cursor.getColumnIndexOrThrow("quantity");
                int stationIndex = cursor.getColumnIndexOrThrow("station");
                int timestampIndex = cursor.getColumnIndexOrThrow("timestamp");

                if (cursor.moveToFirst()) {
                    do {
                        String assignee = cursor.getString(assigneeIndex);
                        String productName = cursor.getString(productNameIndex);
                        String station = cursor.getString(stationIndex);

                        if (assignee.toLowerCase().contains(searchText.toLowerCase()) ||
                                productName.toLowerCase().contains(searchText.toLowerCase()) ||
                                station.toLowerCase().contains(searchText.toLowerCase())) {

                            filteredList.add(new IssuedGoodsItem(
                                    cursor.getInt(idIndex),
                                    assignee,
                                    productName,
                                    cursor.getString(weightIndex),
                                    cursor.getString(flavourIndex),
                                    cursor.getInt(quantityIndex),
                                    station,
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

    @Override
    public void onEditClicked(IssuedGoodsItem item) {
        // Navigate to IssueGoodsFragment with edit mode and pass data
        Bundle bundle = new Bundle();
        bundle.putInt("EDIT_MODE", 1);
        bundle.putInt("ISSUED_ID", item.getId());
        bundle.putString("ASSIGNEE", item.getAssignee());
        bundle.putString("PRODUCT_NAME", item.getProductName());
        bundle.putString("WEIGHT", item.getWeight());
        bundle.putString("FLAVOUR", item.getFlavour());
        bundle.putInt("QUANTITY", item.getQuantity());
        bundle.putString("STATION", item.getStation());

        // Navigate to IssueGoodsFragment
        IssueGoodsFragment issueGoodsFragment = new IssueGoodsFragment();
        issueGoodsFragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, issueGoodsFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDeleteClicked(IssuedGoodsItem item) {
        // Show confirmation dialog before deletion
        showDeleteConfirmationDialog(item);
    }


    private void showDeleteConfirmationDialog(IssuedGoodsItem item) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getActivity());
        builder.setTitle("Delete Issued Goods");
        builder.setMessage("Are you sure you want to delete this issued goods record?\n\n" +
                "Product: " + item.getProductName() + "\n" +
                "Assignee: " + item.getAssignee() + "\n" +
                "Quantity: " + item.getQuantity() + " pieces");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteIssuedGoods(item);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    private void deleteIssuedGoods(IssuedGoodsItem item) {
        boolean isDeleted = dbHelper.deleteIssuedGoods(item.getId(), item.getProductName(), item.getQuantity());

        if (isDeleted) {
            Toast.makeText(getActivity(), "Issued goods deleted successfully", Toast.LENGTH_SHORT).show();
            loadIssuedGoods(); // Refresh the list
        } else {
            Toast.makeText(getActivity(), "Failed to delete issued goods", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}