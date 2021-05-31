package com.simpmangareader.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.simpmangareader.R;
import com.simpmangareader.provider.data.Chapter;
import com.simpmangareader.provider.data.Manga;
import com.simpmangareader.provider.mangadex.Mangadex;
import com.simpmangareader.util.GridAutoFitLayoutManager;
import com.simpmangareader.util.ItemClickSupport;
import com.simpmangareader.util.RecyclerViewAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static androidx.recyclerview.widget.DividerItemDecoration.VERTICAL;

public class Fragment_browse extends Fragment {
    //TODO ; fix duplicate issue when loading mangas, when scrolling to the bottom of the screen
    private final ArrayList<Manga> mData = new ArrayList<>();
    protected Fragment_browse.LayoutManagerType mCurrentLayoutManagerType;
    protected RecyclerView mRecyclerView;
    protected RecyclerViewAdapter mAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;
    private static final String TAG = "RecyclerViewFragment";
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";
    private static final int SPAN_COUNT = 4;
    private static final int COLUMN_WIDTH = 130;
    private static final int DATASET_COUNT = 60;

    int count = 0;
    int currentIndex= 0, currentLimit = 1;
    boolean is_loading = false;


    public enum LayoutManagerType {
        GRID_LAYOUT_MANAGER,
        LINEAR_LAYOUT_MANAGER
    }

    private final Handler myHandler = HandlerCompat.createAsync(Looper.myLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browse, container, false);
        initRecyclerView(rootView, savedInstanceState);



        //TODO: the data will not be immediately available, we need to display something until the data is ready...
        if (mData.size() == 0) {
            //fetch data only if there are no manga, unless it's for reloading
            Mangadex.FetchManga(currentIndex, currentLimit, result -> {
                //NOTE(Mouad): result is an array of Manga
                //UI UPDATED
                synchronized (mData) {
                    mData.addAll(Arrays.asList(result));
                }
                synchronized (mAdapter) {
                    mAdapter.notifyDataSetChanged();
                }
                synchronized (mRecyclerView) {
                    mRecyclerView.notifyAll();
                }

                currentIndex = currentLimit + 1;
                currentLimit = currentIndex + 10;

            }, e -> {
                //TODO: report failure
            }, myHandler);
        }
        return rootView;
    }



    public void initRecyclerView(View rootView, Bundle savedInstanceState){
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_browse_recycler_view);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mCurrentLayoutManagerType = Fragment_browse.LayoutManagerType.GRID_LAYOUT_MANAGER;
        if (savedInstanceState != null) {
            // Restore saved layout manager type.
            mCurrentLayoutManagerType = (Fragment_browse.LayoutManagerType) savedInstanceState
                    .getSerializable(KEY_LAYOUT_MANAGER);
        }
        setRecyclerViewLayoutManager(mCurrentLayoutManagerType);
        mAdapter = new RecyclerViewAdapter(mData);
        mRecyclerView.setAdapter(mAdapter);
        DividerItemDecoration itemDecor = new DividerItemDecoration(getContext(), VERTICAL);
        mRecyclerView.addItemDecoration(itemDecor);

        this.configureOnClickRecyclerView();
        this.configureOnLongClickRecyclerView();


    }

    /**
     * Set RecyclerView's LayoutManager to the one given.
     *
     * @param layoutManagerType Type of layout manager to switch to.
     */
    public void setRecyclerViewLayoutManager(Fragment_browse.LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }

        if (layoutManagerType == Fragment_browse.LayoutManagerType.GRID_LAYOUT_MANAGER) {
            mLayoutManager = new GridAutoFitLayoutManager(getActivity(), COLUMN_WIDTH);
            mCurrentLayoutManagerType = Fragment_browse.LayoutManagerType.GRID_LAYOUT_MANAGER;
        } else {
            mLayoutManager = new LinearLayoutManager(getActivity());
            mCurrentLayoutManagerType = Fragment_browse.LayoutManagerType.LINEAR_LAYOUT_MANAGER;
        }

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                final int visibleThreshold = 2;

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                assert layoutManager != null;
                int lastItem = layoutManager.findLastCompletelyVisibleItemPosition();
                int currentTotalCount = layoutManager.getItemCount();


                if (currentTotalCount <= lastItem + visibleThreshold && !is_loading) {
                    is_loading = true;

                    System.out.println("LOADING MORE from " + currentIndex + "to " + currentLimit);
                    Mangadex.FetchManga(currentIndex, currentLimit, result -> {
                        //NOTE(Mouad): result is an array of Manga
                        //UI UPDATED
                        synchronized (mData) {
                            for(int i = 0; i <= 10; ++i)
                                mData.add(result[i]);

                        }
                        synchronized (mAdapter) {
                            mAdapter.notifyDataSetChanged();
                        }
                        synchronized (mRecyclerView) {
                            mRecyclerView.notifyAll();
                        }


                        System.out.println("size : " + mData.size());
                        System.out.printf("currLimint : " + currentLimit);
                        is_loading = false;
                        currentIndex = currentLimit + 1;
                        currentLimit = currentIndex + 10;


                    }, e -> {
                        //TODO: report failure
                    }, myHandler);


                }
            }
        });
    }

    private void configureOnLongClickRecyclerView() {
        ItemClickSupport.addTo(mRecyclerView, R.layout.activity_main)
                .setOnItemLongClickListener((recyclerView, position, v) -> {
                    Toast.makeText(getContext(), "long clicked \"Position : \""+position, Toast.LENGTH_LONG).show();

                    return true;
                });
    }

    private void configureOnClickRecyclerView()
    {
        ItemClickSupport.addTo(mRecyclerView, R.layout.activity_main)
                .setOnItemClickListener((recyclerView, position, v) -> {
                    Toast.makeText(getContext(), "short clicked \"Position : \""+position, Toast.LENGTH_LONG).show();

                    //passing args and starting chapter detail activity
                    Intent intent = new Intent(getContext(), MangaDetailActivity.class);
                    Bundle bundle = new Bundle();
                    synchronized (mData.get(position)) {
                        bundle.putParcelable("mangas", mData.get(position));
                    }
                    intent.putExtras(bundle);
                    startActivity(intent);
                });
    }




    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save currently selected layout manager.
        savedInstanceState.putSerializable(KEY_LAYOUT_MANAGER, mCurrentLayoutManagerType);
        super.onSaveInstanceState(savedInstanceState);
    }



}

