package com.varren.animationdemo;

import android.content.Context;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private List<Integer> mItems = new ArrayList<>();
    private DefaultItemAnimator animator = new DefaultItemAnimator();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.recycleView);

        //generating random data
        for (int i = 0; i < 50; i++) mItems.add(i);

        //recyclerView setup
        MyAdapter adapter = new MyAdapter();
        adapter.setHasStableIds(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(animator);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void sort() {
        for (int i = 0, j = mItems.size() - 1; i < j; i++, j--)
            Collections.swap(mItems, i, j);

        MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
        //adapter.notifyItemRangeChanged(0, mItems.size());

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        int itemsChanged = lastVisible - firstVisible + 1; // + 1 because we start count items from 0
        int start = firstVisible - itemsChanged> 0 ? firstVisible - itemsChanged: 0;
        adapter.notifyItemRangeChanged(start, itemsChanged+itemsChanged);

        //adapter.notifyItemRangeChanged(0, mItems.size());
    }

    /************************************************************************************************
     * simple RecyclerView Adapter
     *************************************************************************************************/
    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        Random random = new Random();
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
            //Log.e("onCreateViewHolder","onCreateViewHolder") ;
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final MyAdapter.ViewHolder holder, int position) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("test","onclickk getAdapterPosition " + holder.getAdapterPosition());
                    Log.e("test","onclickk getItemId " + holder.getItemId());
                }
            });
           // Log.e("onBindViewHolder","onBindViewHolder" + holder.getAdapterPosition());
            holder.titleView.setText(("Cell " + mItems.get(position)));
            //holder.titleView.setTextSize(5 + mItems.get(position));
            holder.titleView.setTextSize(5 + 50*(random.nextInt(2)));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleView;

            public ViewHolder(View itemView) {
                super(itemView);
                titleView = (TextView) itemView.findViewById(R.id.titleTextView);
            }
        }

        @Override
        public long getItemId(int position) {
            return mItems.get(position);
        }
    }

    /************************************************************************************************
     * MENU
     *************************************************************************************************/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
        switch (item.getItemId()) {
            case R.id.action_sort:
                Log.e("On PRESS","SORT");
                sort();
                return true;
            case R.id.action_add:
                Log.e("On PRESS","ADD");
                mItems.add(2, mItems.size());
                adapter.notifyItemInserted(2);
                return true;
            case R.id.action_remove:
                Log.e("On PRESS","REMOVE");
                mItems.remove(2);
                adapter.notifyItemRemoved(2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
}