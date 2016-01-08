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

import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {

    Spinner mDifficultyPicker;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (TileActivity.hasSaveData(this)) {
            // If we have saved data, launch immediately into TileActivity
            startActivity(new Intent(this, TileActivity.class));
            finish();
        }

        setContentView(R.layout.activity_main);

        mDifficultyPicker = (Spinner) findViewById(R.id.difficulty);
        mDifficultyPicker.setSelection(PreferenceManager.getDefaultSharedPreferences(this).getInt(Utils.PREF_DIFFICULTY, 1));
        mDifficultyPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putInt(Utils.PREF_DIFFICULTY, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        RecyclerView imageGrid = (RecyclerView) findViewById(R.id.image_grid);
        imageGrid.setLayoutManager(new GridLayoutManager(this, 2));
        imageGrid.setAdapter(new ImageAdapter(this));
    }

    public void onImageSelected(int imageId) {
        int[] sizes = getResources().getIntArray(R.array.difficulty_values);
        int size = sizes[mDifficultyPicker.getSelectedItemPosition()];

        startActivity(new Intent(this, TileActivity.class)
                        .putExtra(TileActivity.KEY_SIZE, size)
                        .putExtra(TileActivity.KEY_IMAGE_RESOURCE, imageId)
        );
        TileActivity.clearSaveData(this);
        finish();
    }


}

class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    final int[] IMAGE_IDS = {
            R.raw.bruce,
            R.raw.skiing,
            R.raw.cartoon,
    };

    MainActivity mContext;

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

    ImageAdapter(MainActivity context) {
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent, false);
        return new ViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Picasso.with(mContext).load(IMAGE_IDS[position]).centerCrop().fit().placeholder(android.R.color.transparent).into(holder.image);
    }

    @Override
    public int getItemCount() {
        return IMAGE_IDS.length;
    }

    public void onItemClicked(int position) {
        mContext.onImageSelected(IMAGE_IDS[position]);
    }
}