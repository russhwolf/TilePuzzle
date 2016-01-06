package io.intrepid.russell.tilepuzzle;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class TileActivity extends AppCompatActivity {
    private static final String TAG = TileActivity.class.getSimpleName();

    public static final String EXTRA_IMAGE_RESOURCE = "image_resource";
    public static final String EXTRA_SIZE = "size";

    private RecyclerView mTileGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tile);

        int imageResource = getIntent().getIntExtra(EXTRA_IMAGE_RESOURCE, R.raw.bruce);
        int size = getIntent().getIntExtra(EXTRA_SIZE, 4); // If we can't read anything, default is medium

        mTileGrid = (RecyclerView) findViewById(R.id.tile_grid);

        initializeGridAsync(imageResource, size);
    }

    private void initializeGridAsync(int imageResource, int size) {
        Log.d(TAG, "Initializing Tile Grid...");
        new AsyncTask<Integer, Void, TileAdapter>() {

            @Override
            protected TileAdapter doInBackground(Integer... params) {
                int imageResource = params[0];
                int size = params[1];

                Bitmap raw = Utils.decodeSampledBitmapFromResource(getResources(), imageResource, 500, 500);
                Bitmap[] tiles = generateTiles(raw, size);
                return new TileAdapter(size, tiles);
            }

            @Override
            protected void onPostExecute(TileAdapter adapter) {
                mTileGrid.setLayoutManager(new GridLayoutManager(TileActivity.this, adapter.mSize));
                mTileGrid.setAdapter(adapter);
            }
        }.execute(imageResource, size);

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
    private static final String TAG = TileAdapter.class.getSimpleName();

    final int mMissingValue;
    final int mSize;
    final int[] mValues;
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
            int value = mValues[position];
            int size = mSize;

//            Toast.makeText(v.getContext(), "Item "+position+" clicked (value = "+value+")", Toast.LENGTH_SHORT).show();

            // Check if we're next to empty block
            if (position >= size) {
                // We're not in the first row, so there's a square above;
                int above = position - size;
                if (mValues[above] == mMissingValue) {
                    swap(position, above);
                    return;
                }
            }
            if (position < size * (size - 1)) {
                // We're not in the last row, so there's a square below;
                int below = position + size;
                if (mValues[below] == mMissingValue) {
                    swap(position, below);
                    return;
                }
            }
            if (position % size > 0) {
                // We're not in the first column, so there's a square to the left
                int left = position - 1;
                if (mValues[left] == mMissingValue) {
                    swap(position, left);
                    return;
                }
            }
            if (position % size < size - 1) {
                // We're not in the last column, so there's a square to the right
                int right = position + 1;
                if (mValues[right] == mMissingValue) {
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
        mValues = new int[size * size];
        for (int i = 0; i < mSize * mSize; i++) {
            mValues[i] = i;
        }

        mMissingValue = size * size - 1;

        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                int seconds = params[0];
                try {
                    Thread.sleep(seconds * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // TODO it would be nice to do this randomly and check if the shuffle is valid
                // However the math for this is a pain.
                int numTiles = mValues.length - 1; // Not including empty!
                for (int i = 0; i < numTiles; i++) {
                    int newValue = numTiles - 1 - i;
                    if (newValue < 0) {
                        newValue += numTiles;
                    }
                    mValues[i] = newValue;
                }
                if (mSize % 2 == 0) {
                    swap(numTiles - 2, numTiles - 1, false);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mStarted = true;
                notifyDataSetChanged();
            }
        }.execute(1);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int value = mValues[position];
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
        return mValues.length;
    }

    @Override
    public long getItemId(int position) {
        return mValues[position];
    }

    private void swap(int position1, int position2) {
        swap(position1, position2, true);
    }

    private void swap(int position1, int position2, boolean notify) {
        int value1 = mValues[position1];
        int value2 = mValues[position2];
        mValues[position1] = value2;
        mValues[position2] = value1;
        if (notify) {
            notifyDataSetChanged();
        }
    }

}



