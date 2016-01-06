package io.intrepid.russell.tilepuzzle;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TileActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_RESOURCE = "image_resource";
    public static final String EXTRA_SIZE = "size";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tile);

        int imageResource = getIntent().getIntExtra(EXTRA_IMAGE_RESOURCE, R.raw.bruce);
        int size = getIntent().getIntExtra(EXTRA_SIZE, 4);

        Bitmap raw = Utils.decodeSampledBitmapFromResource(getResources(), imageResource, 500, 500); // TODO make this size real
        Bitmap[] tiles = generateTiles(raw, size);

        RecyclerView tileGrid = (RecyclerView) findViewById(R.id.tile_grid);
        tileGrid.setLayoutManager(new GridLayoutManager(this, size));
        tileGrid.setAdapter(new TileAdapter(size, tiles));
    }

    private static Bitmap cropSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == height) return bitmap; // short-circuit if we're already square

        int min = Math.min(width, height);

        return Bitmap.createBitmap(bitmap, (width - min) / 2, (height - min) / 2, min, min);
    }

    private static Bitmap[] generateTiles(Bitmap bitmap, int size) {
        bitmap = cropSquare(bitmap);
        int tileSide = bitmap.getWidth() / size;
        Bitmap[] bitmaps = new Bitmap[size * size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int index = j * size + i % size;
                bitmaps[index] = Bitmap.createBitmap(bitmap, i * tileSide, j * tileSide, tileSide, tileSide);
            }
        }
        return bitmaps;
    }

}

class TileAdapter extends RecyclerView.Adapter<TileAdapter.ViewHolder> {

    final int mMissingValue;
    final int mSize;
    final List<Integer> mValues;
    final Bitmap[] mTiles;

    boolean mStarted = false;

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView image;

        ViewHolder(View itemView) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.image);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            int value = mValues.get(position);
            int size = mSize;

//            Toast.makeText(v.getContext(), "Item "+position+" clicked (value = "+value+")", Toast.LENGTH_SHORT).show();

            // Check if we're next to empty block
            if (position >= size) {
                // We're not in the first row, so there's a square above;
                int above = position - size;
                if (mValues.get(above) == mMissingValue) {
                    swap(position, above);
                    return;
                }
            }
            if (position < size * (size - 1)) {
                // We're not in the last row, so there's a square below;
                int below = position + size;
                if (mValues.get(below) == mMissingValue) {
                    swap(position, below);
                    return;
                }
            }
            if (position % size > 0) {
                // We're not in the first column, so there's a square to the left
                int left = position - 1;
                if (mValues.get(left) == mMissingValue) {
                    swap(position, left);
                    return;
                }
            }
            if (position % size < size - 1) {
                // We're not in the last column, so there's a square to the right
                int right = position + 1;
                if (mValues.get(right) == mMissingValue) {
                    swap(position, right);
                    return;
                }
            }

        }
    }

    TileAdapter(int size, Bitmap[] tiles) {
        setHasStableIds(true); // This gives us animation when moving tiles

        mSize = size;
        mTiles = tiles;
        mValues = new ArrayList<>(size * size);
        for (int i = 0; i < mSize * mSize; i++) {
            mValues.add(i);
        }

        mMissingValue = size * size - 1;

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Collections.shuffle(mValues); // TODO shuffle better
                mStarted = true;
                return null;
            }

            @Override
            protected void onPostExecute(Void param) {
                notifyDataSetChanged();
            }
        }.execute();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int value = mValues.get(position);
        if (value == mMissingValue) {
            holder.itemView.setClickable(false);
            holder.image.setImageResource(android.R.color.transparent);
        } else {
            holder.itemView.setClickable(mStarted);
            holder.image.setImageBitmap(mTiles[value]);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    @Override
    public long getItemId(int position) {
        return mValues.get(position);
    }

    private void swap(int position1, int position2) {
        int value1 = mValues.get(position1);
        int value2 = mValues.get(position2);
        mValues.set(position1, value2);
        mValues.set(position2, value1);
        notifyDataSetChanged();
    }

}



