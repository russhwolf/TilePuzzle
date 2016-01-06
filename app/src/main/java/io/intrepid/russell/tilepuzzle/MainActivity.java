package io.intrepid.russell.tilepuzzle;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity implements OnImageSelectedListener {
    public static final String PREF_DIFFICULTY = "difficulty";

    Spinner mDifficultyPicker;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDifficultyPicker = (Spinner) findViewById(R.id.difficulty);
        mDifficultyPicker.setSelection(PreferenceManager.getDefaultSharedPreferences(this).getInt(PREF_DIFFICULTY, 1));
        mDifficultyPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putInt(PREF_DIFFICULTY, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        RecyclerView imageGrid = (RecyclerView) findViewById(R.id.image_grid);
        imageGrid.setLayoutManager(new GridLayoutManager(this, 2));
        imageGrid.setAdapter(new ImageAdapter(this, this));
    }

    @Override
    public void onImageSelected(int imageId) {
        int[] sizes = getResources().getIntArray(R.array.difficulty_values);
        int size = sizes[mDifficultyPicker.getSelectedItemPosition()];

        startActivity(new Intent(this, TileActivity.class)
                        .putExtra(TileActivity.EXTRA_SIZE, size)
                        .putExtra(TileActivity.EXTRA_IMAGE_RESOURCE, imageId)
        );
    }


}

class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    static final int[] sImageIds = {
            R.raw.bruce,
            R.raw.skiing,
            R.raw.cartoon,
    };

    AppCompatActivity mContext;
    OnImageSelectedListener mListener;

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;

        ViewHolder(View itemView, final ImageAdapter adapter) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.image);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.onItemClicked(getAdapterPosition());
                }
            });
        }
    }

    ImageAdapter(AppCompatActivity context, OnImageSelectedListener listener) {
        mListener = listener;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent, false);
        return new ViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Glide.with(mContext).load(sImageIds[position]).placeholder(android.R.color.transparent).into(holder.image);
    }

    @Override
    public int getItemCount() {
        return sImageIds.length;
    }

    public void onItemClicked(int position) {
        mListener.onImageSelected(sImageIds[position]);
    }
}

interface OnImageSelectedListener {
    void onImageSelected(int position);
}